# ==============================================================================
# NEXORA AWS SECURITY GROUPS (ZERO-TRUST TIER ISOLATION)
# ALB SG (0.0.0.0/0 on 443) -> ECS SG (from ALB only) -> DB/Redis SG (from ECS only)
# ==============================================================================

resource "aws_security_group" "alb_sg" {
  name        = "nexora-alb-sg"
  description = "Public internet ingress for HTTPS traffic"
  vpc_id      = aws_vpc.nexora_vpc.id

  ingress {
    description = "Allow HTTPS from anywhere"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs_sg" {
  name        = "nexora-ecs-sg"
  description = "Allows traffic only from ALB into application containers"
  vpc_id      = aws_vpc.nexora_vpc.id

  ingress {
    description     = "Traffic from ALB on port 8080"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "db_sg" {
  name        = "nexora-db-sg"
  description = "Allows MySQL traffic strictly from ECS application tasks"
  vpc_id      = aws_vpc.nexora_vpc.id

  ingress {
    description     = "MySQL 3306 from ECS tasks only"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_sg.id]
  }
}

resource "aws_security_group" "redis_sg" {
  name        = "nexora-redis-sg"
  description = "Allows Redis traffic strictly from ECS application tasks"
  vpc_id      = aws_vpc.nexora_vpc.id

  ingress {
    description     = "Redis 6379 from ECS tasks only"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_sg.id]
  }
}
