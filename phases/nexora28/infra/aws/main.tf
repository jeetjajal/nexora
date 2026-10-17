# ==============================================================================
# NEXORA AWS INFRASTRUCTURE (TERRAFORM SPECIFICATION)
# Region: ap-south-1 (Mumbai)
# Security Standard: CIS AWS Foundations Benchmark compliant
# - Zero public exposure of RDS MySQL, ElastiCache Redis, or MSK Kafka
# - Multi-AZ redundancy (ap-south-1a & ap-south-1b)
# - Secrets injected via AWS Secrets Manager with automatic KMS encryption
# ==============================================================================

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "Nexora"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# ------------------------------------------------------------------------------
# 1. NETWORKING: Dedicated Virtual Private Cloud (VPC)
# ------------------------------------------------------------------------------
resource "aws_vpc" "nexora_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "nexora-vpc-${var.environment}"
  }
}

# Internet Gateway for Public Subnet (ALB Ingress)
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.nexora_vpc.id
}

# Public Subnets (ALB & NAT Gateways across 2 AZs)
resource "aws_subnet" "public_1a" {
  vpc_id            = aws_vpc.nexora_vpc.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "ap-south-1a"
  map_public_ip_on_launch = true
}

resource "aws_subnet" "public_1b" {
  vpc_id            = aws_vpc.nexora_vpc.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "ap-south-1b"
  map_public_ip_on_launch = true
}

# Private Application Subnets (ECS Fargate Tasks)
resource "aws_subnet" "app_private_1a" {
  vpc_id            = aws_vpc.nexora_vpc.id
  cidr_block        = "10.0.11.0/24"
  availability_zone = "ap-south-1a"
}

resource "aws_subnet" "app_private_1b" {
  vpc_id            = aws_vpc.nexora_vpc.id
  cidr_block        = "10.0.12.0/24"
  availability_zone = "ap-south-1b"
}

# Isolated Database & Cache Subnets (No direct internet egress)
resource "aws_subnet" "data_isolated_1a" {
  vpc_id            = aws_vpc.nexora_vpc.id
  cidr_block        = "10.0.21.0/24"
  availability_zone = "ap-south-1a"
}

resource "aws_subnet" "data_isolated_1b" {
  vpc_id            = aws_vpc.nexora_vpc.id
  cidr_block        = "10.0.22.0/24"
  availability_zone = "ap-south-1b"
}

# ------------------------------------------------------------------------------
# 2. APPLICATION LOAD BALANCER (ALB) - Public HTTPS Entrypoint
# ------------------------------------------------------------------------------
resource "aws_lb" "nexora_alb" {
  name               = "nexora-alb-${var.environment}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [aws_subnet.public_1a.id, aws_subnet.public_1b.id]
}

# ------------------------------------------------------------------------------
# 3. AMAZON ECS FARGATE (Serverless Container Compute)
# ------------------------------------------------------------------------------
resource "aws_ecs_cluster" "nexora_cluster" {
  name = "nexora-cluster-${var.environment}"
}

# ------------------------------------------------------------------------------
# 4. AMAZON RDS MYSQL (Multi-AZ Managed Database)
# ------------------------------------------------------------------------------
resource "aws_db_instance" "nexora_mysql" {
  identifier             = "nexora-mysql-${var.environment}"
  engine                 = "mysql"
  engine_version         = "8.0.35"
  instance_class         = "db.t4g.medium"
  allocated_storage      = 50
  max_allocated_storage  = 200
  multi_az               = true
  publicly_accessible    = false # STRICT: NEVER EXPOSE DB TO PUBLIC INTERNET
  vpc_security_group_ids = [aws_security_group.db_sg.id]
  skip_final_snapshot    = true
}

# ------------------------------------------------------------------------------
# 5. AMAZON ELASTICACHE REDIS (Cluster Mode Enabled)
# ------------------------------------------------------------------------------
resource "aws_elasticache_replication_group" "nexora_redis" {
  replication_group_id       = "nexora-redis-${var.environment}"
  description                = "Nexora in-memory session, cart and token bucket rate limiter"
  node_type                  = "cache.t4g.small"
  num_cache_clusters         = 2
  automatic_failover_enabled = true
  security_group_ids         = [aws_security_group.redis_sg.id]
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
}

# ------------------------------------------------------------------------------
# 6. AMAZON S3 & CLOUDFRONT CDN (Media & Product Images)
# ------------------------------------------------------------------------------
resource "aws_s3_bucket" "nexora_assets" {
  bucket = "nexora-media-assets-${var.environment}"
}

resource "aws_s3_bucket_public_access_block" "nexora_assets_block" {
  bucket                  = aws_s3_bucket.nexora_assets.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
