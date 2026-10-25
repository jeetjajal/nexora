export interface Product {
  id: string;
  name: string;
  categoryId: string;
  price: number;
  mrp: number;
  discount: number;
  unit: string;
  image: string;
  rating: number;
  reviewsCount: number;
  inStock: boolean;
  deliveryTime: string;
  badge?: string;
  brand?: string;
}

export const PRODUCTS: Product[] = [
  // Dairy & Bread
  {
    id: 'p1',
    name: 'Amul Taaza Fresh Toned Milk (Pouch)',
    categoryId: '1',
    price: 27,
    mrp: 28,
    discount: 4,
    unit: '500 ml',
    image: 'https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=500&q=80',
    rating: 4.9,
    reviewsCount: 1420,
    inStock: true,
    deliveryTime: '8 MINS',
    badge: 'Bestseller',
    brand: 'Amul'
  },
  {
    id: 'p2',
    name: 'Harvest Gold 100% Whole Wheat Bread',
    categoryId: '1',
    price: 48,
    mrp: 55,
    discount: 13,
    unit: '400 g',
    image: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=500&q=80',
    rating: 4.8,
    reviewsCount: 890,
    inStock: true,
    deliveryTime: '9 MINS',
    brand: 'Harvest Gold'
  },
  {
    id: 'p3',
    name: 'Farm Fresh White Eggs (Pack of 6)',
    categoryId: '1',
    price: 52,
    mrp: 65,
    discount: 20,
    unit: '6 pcs',
    image: 'https://images.unsplash.com/photo-1516448620398-c5f44bf9f441?auto=format&fit=crop&w=500&q=80',
    rating: 4.7,
    reviewsCount: 650,
    inStock: true,
    deliveryTime: '8 MINS',
    badge: 'High Protein'
  },
  {
    id: 'p4',
    name: 'Amul Salted Butter Block',
    categoryId: '1',
    price: 56,
    mrp: 60,
    discount: 7,
    unit: '100 g',
    image: 'https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?auto=format&fit=crop&w=500&q=80',
    rating: 4.9,
    reviewsCount: 1120,
    inStock: true,
    deliveryTime: '8 MINS',
    brand: 'Amul'
  },

  // Fresh Vegetables
  {
    id: 'p5',
    name: 'Hybrid Fresh Red Tomatoes',
    categoryId: '2',
    price: 34,
    mrp: 50,
    discount: 32,
    unit: '500 g',
    image: 'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=500&q=80',
    rating: 4.6,
    reviewsCount: 940,
    inStock: true,
    deliveryTime: '9 MINS',
    badge: 'Farm Fresh'
  },
  {
    id: 'p6',
    name: 'Farm Fresh Onion (Pyaz)',
    categoryId: '2',
    price: 38,
    mrp: 55,
    discount: 31,
    unit: '1 kg',
    image: 'https://images.unsplash.com/photo-1618512496248-a07fe83aa8cb?auto=format&fit=crop&w=500&q=80',
    rating: 4.8,
    reviewsCount: 1350,
    inStock: true,
    deliveryTime: '8 MINS',
    badge: 'Daily Essential'
  },
  {
    id: 'p7',
    name: 'Fresh English Cucumber (Kheera)',
    categoryId: '2',
    price: 24,
    mrp: 35,
    discount: 31,
    unit: '500 g',
    image: 'https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?auto=format&fit=crop&w=500&q=80',
    rating: 4.5,
    reviewsCount: 310,
    inStock: true,
    deliveryTime: '10 MINS'
  },
  {
    id: 'p8',
    name: 'Green Capsicum (Shimla Mirch)',
    categoryId: '2',
    price: 29,
    mrp: 45,
    discount: 35,
    unit: '250 g',
    image: 'https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?auto=format&fit=crop&w=500&q=80',
    rating: 4.7,
    reviewsCount: 420,
    inStock: true,
    deliveryTime: '9 MINS'
  },

  // Fresh Fruits
  {
    id: 'p9',
    name: 'Shimla Royal Delicious Red Apples',
    categoryId: '3',
    price: 139,
    mrp: 180,
    discount: 23,
    unit: '4 pcs (approx 500-600g)',
    image: 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=500&q=80',
    rating: 4.8,
    reviewsCount: 780,
    inStock: true,
    deliveryTime: '9 MINS',
    badge: 'Crispy & Sweet'
  },
  {
    id: 'p10',
    name: 'Robusta Fresh Bananas',
    categoryId: '3',
    price: 42,
    mrp: 60,
    discount: 30,
    unit: '6 pcs',
    image: 'https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=500&q=80',
    rating: 4.9,
    reviewsCount: 1560,
    inStock: true,
    deliveryTime: '8 MINS',
    badge: 'Instant Energy'
  },
  {
    id: 'p11',
    name: 'Imported Sweet Navel Oranges',
    categoryId: '3',
    price: 99,
    mrp: 140,
    discount: 29,
    unit: '500 g',
    image: 'https://images.unsplash.com/photo-1582979512210-99b6a53386f9?auto=format&fit=crop&w=500&q=80',
    rating: 4.7,
    reviewsCount: 390,
    inStock: true,
    deliveryTime: '10 MINS'
  },

  // Atta, Rice & Grains
  {
    id: 'p12',
    name: 'Aashirvaad Shudh Chakki Atta',
    categoryId: '4',
    price: 245,
    mrp: 275,
    discount: 11,
    unit: '5 kg',
    image: 'https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&w=500&q=80',
    rating: 4.9,
    reviewsCount: 2200,
    inStock: true,
    deliveryTime: '10 MINS',
    badge: '100% Whole Wheat',
    brand: 'Aashirvaad'
  },
  {
    id: 'p13',
    name: 'Fortune Everyday Rozana Basmati Rice',
    categoryId: '4',
    price: 115,
    mrp: 145,
    discount: 21,
    unit: '1 kg',
    image: 'https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=500&q=80',
    rating: 4.7,
    reviewsCount: 880,
    inStock: true,
    deliveryTime: '10 MINS',
    brand: 'Fortune'
  },
  {
    id: 'p14',
    name: 'Tata Sampann Unpolished Toor Dal',
    categoryId: '4',
    price: 178,
    mrp: 210,
    discount: 15,
    unit: '1 kg',
    image: 'https://images.unsplash.com/photo-1543339308-43e59d6b73a6?auto=format&fit=crop&w=500&q=80',
    rating: 4.8,
    reviewsCount: 640,
    inStock: true,
    deliveryTime: '9 MINS',
    brand: 'Tata Sampann'
  },

  // Snacks & Munchies
  {
    id: 'p15',
    name: "Lay's India's Magic Masala Potato Chips",
    categoryId: '7',
    price: 20,
    mrp: 20,
    discount: 0,
    unit: '50 g',
    image: 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?auto=format&fit=crop&w=500&q=80',
    rating: 4.8,
    reviewsCount: 3100,
    inStock: true,
    deliveryTime: '8 MINS',
    badge: 'Hot Seller',
    brand: "Lay's"
  },
  {
    id: 'p16',
    name: 'Doritos Cheese Supreme Nachos',
    categoryId: '7',
    price: 50,
    mrp: 60,
    discount: 17,
    unit: '75 g',
    image: 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=500&q=80',
    rating: 4.7,
    reviewsCount: 920,
    inStock: true,
    deliveryTime: '8 MINS',
    brand: 'Doritos'
  },
  {
    id: 'p17',
    name: 'Haldiram’s Nagpur Aloo Bhujia',
    categoryId: '7',
    price: 49,
    mrp: 55,
    discount: 11,
    unit: '200 g',
    image: 'https://images.unsplash.com/photo-1601050690597-df0568f70950?auto=format&fit=crop&w=500&q=80',
    rating: 4.9,
    reviewsCount: 1450,
    inStock: true,
    deliveryTime: '9 MINS',
    brand: 'Haldirams'
  },

  // Cold Drinks & Juices
  {
    id: 'p18',
    name: 'Coca-Cola Zero Sugar Can',
    categoryId: '8',
    price: 40,
    mrp: 40,
    discount: 0,
    unit: '300 ml',
    image: 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=500&q=80',
    rating: 4.8,
    reviewsCount: 1800,
    inStock: true,
    deliveryTime: '8 MINS',
    brand: 'Coca-Cola'
  },
  {
    id: 'p19',
    name: 'Real Fruit Power 100% Mixed Fruit Juice',
    categoryId: '8',
    price: 110,
    mrp: 130,
    discount: 15,
    unit: '1 L',
    image: 'https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?auto=format&fit=crop&w=500&q=80',
    rating: 4.7,
    reviewsCount: 710,
    inStock: true,
    deliveryTime: '9 MINS',
    brand: 'Real'
  },

  // Tea, Coffee
  {
    id: 'p20',
    name: 'Nescafe Classic Instant Coffee Jar',
    categoryId: '9',
    price: 195,
    mrp: 230,
    discount: 15,
    unit: '50 g',
    image: 'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?auto=format&fit=crop&w=500&q=80',
    rating: 4.9,
    reviewsCount: 1100,
    inStock: true,
    deliveryTime: '8 MINS',
    brand: 'Nescafe'
  },

  // Electronics & Accessories
  {
    id: 'p21',
    name: 'boAt Type-C 65W Fast Charging Nylon Cable',
    categoryId: '31',
    price: 249,
    mrp: 499,
    discount: 50,
    unit: '1.5 m',
    image: 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=500&q=80',
    rating: 4.6,
    reviewsCount: 520,
    inStock: true,
    deliveryTime: '10 MINS',
    badge: '50% OFF',
    brand: 'boAt'
  },
  {
    id: 'p22',
    name: 'Duracell Ultra Alkaline AA Batteries (Pack of 4)',
    categoryId: '22',
    price: 175,
    mrp: 200,
    discount: 13,
    unit: '4 pcs',
    image: 'https://images.unsplash.com/photo-1619725002198-6a689b72f41d?auto=format&fit=crop&w=500&q=80',
    rating: 4.9,
    reviewsCount: 890,
    inStock: true,
    deliveryTime: '8 MINS',
    brand: 'Duracell'
  }
];
