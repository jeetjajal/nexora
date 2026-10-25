export interface Category {
  id: string;
  name: string;
  slug: string;
  icon: string;
  itemCount: number;
  highlight?: boolean;
}

export const CATEGORIES: Category[] = [
  { id: '1', name: 'Dairy, Bread & Eggs', slug: 'dairy-bread-eggs', icon: '🥛', itemCount: 142, highlight: true },
  { id: '2', name: 'Fresh Vegetables', slug: 'fresh-vegetables', icon: '🥦', itemCount: 88, highlight: true },
  { id: '3', name: 'Fresh Fruits', slug: 'fresh-fruits', icon: '🍎', itemCount: 65, highlight: true },
  { id: '4', name: 'Atta, Rice, Dal & Grains', slug: 'atta-rice-dal', icon: '🌾', itemCount: 110 },
  { id: '5', name: 'Oils, Ghee & Masalas', slug: 'oils-ghee-masala', icon: '🫒', itemCount: 95 },
  { id: '6', name: 'Bakery & Biscuits', slug: 'bakery-biscuits', icon: '🍪', itemCount: 130 },
  { id: '7', name: 'Snacks & Munchies', slug: 'snacks-munchies', icon: '🥨', itemCount: 185, highlight: true },
  { id: '8', name: 'Cold Drinks & Juices', slug: 'cold-drinks-juices', icon: '🥤', itemCount: 92 },
  { id: '9', name: 'Tea, Coffee & Health Drinks', slug: 'tea-coffee-health', icon: '☕', itemCount: 74 },
  { id: '10', name: 'Instant & Frozen Food', slug: 'instant-frozen', icon: '🍜', itemCount: 84 },
  { id: '11', name: 'Chocolates & Sweets', slug: 'chocolates-sweets', icon: '🍫', itemCount: 68 },
  { id: '12', name: 'Dry Fruits, Nuts & Seeds', slug: 'dry-fruits-seeds', icon: '🥜', itemCount: 53 },
  { id: '13', name: 'Sauces, Spreads & Dips', slug: 'sauces-spreads', icon: '🍯', itemCount: 49 },
  { id: '14', name: 'Ice Creams & Desserts', slug: 'ice-creams-desserts', icon: '🍨', itemCount: 42 },
  { id: '15', name: 'Personal Care & Grooming', slug: 'personal-care', icon: '🧴', itemCount: 115 },
  { id: '16', name: 'Bath & Body', slug: 'bath-body', icon: '🧼', itemCount: 60 },
  { id: '17', name: 'Hair Care & Styling', slug: 'hair-care', icon: '💆', itemCount: 55 },
  { id: '18', name: 'Oral Care & Hygiene', slug: 'oral-care', icon: '🪥', itemCount: 44 },
  { id: '19', name: 'Cleaning & Household', slug: 'cleaning-household', icon: '🧹', itemCount: 80 },
  { id: '20', name: 'Laundry & Dishwash', slug: 'laundry-dishwash', icon: '🫧', itemCount: 62 },
  { id: '21', name: 'Kitchen & Dining Needs', slug: 'kitchen-dining', icon: '🍳', itemCount: 78 },
  { id: '22', name: 'Home & Electrical Essentials', slug: 'home-electrical', icon: '💡', itemCount: 39 },
  { id: '23', name: 'Baby Care & Diapers', slug: 'baby-care', icon: '👶', itemCount: 58 },
  { id: '24', name: 'Pet Supplies & Food', slug: 'pet-supplies', icon: '🐾', itemCount: 37 },
  { id: '25', name: 'Pharma & First Aid', slug: 'pharma-wellness', icon: '💊', itemCount: 51 },
  { id: '26', name: 'Stationery & Office Supplies', slug: 'stationery-office', icon: '✏️', itemCount: 46 },
  { id: '27', name: 'Beauty & Cosmetics', slug: 'beauty-cosmetics', icon: '💄', itemCount: 72 },
  { id: '28', name: 'Sexual Wellness', slug: 'sexual-wellness', icon: '🛡️', itemCount: 34 },
  { id: '29', name: 'Paan & Party Corner', slug: 'paan-party', icon: '🎉', itemCount: 30 },
  { id: '30', name: 'Pooja & Festive Needs', slug: 'pooja-festive', icon: '🪔', itemCount: 40 },
  { id: '31', name: 'Electronics & Mobile Cables', slug: 'electronics-accessories', icon: '⚡', itemCount: 66 },
];
