/**
 * In-browser copy of the verified nutrition table (per 100 g), ported from the
 * backend's `nutrition_db.py`. Used only by the offline fallback engine; when
 * the backend is reachable, reconciliation happens server-side via MCP.
 */

export interface NutritionRow {
  food: string;
  matched: boolean;
  grams: number;
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
}

// name -> [kcal, protein, carbs, fat] per 100 g
const TABLE: Record<string, [number, number, number, number]> = {
  "grilled chicken breast": [165, 31.0, 0.0, 3.6],
  "grilled chicken thigh": [209, 26.0, 0.0, 11.0],
  "chicken curry": [180, 14.0, 6.0, 11.0],
  "white rice": [130, 2.7, 28.0, 0.3],
  "basmati rice": [121, 3.0, 25.0, 0.4],
  "brown rice": [112, 2.6, 24.0, 0.9],
  naan: [310, 9.0, 50.0, 6.0],
  roti: [297, 11.0, 46.0, 7.0],
  dal: [116, 9.0, 20.0, 0.4],
  paneer: [265, 18.0, 1.2, 20.0],
  "mixed salad": [60, 1.5, 5.0, 3.5],
  olives: [115, 0.8, 6.0, 11.0],
  "margherita pizza": [266, 11.0, 33.0, 10.0],
  porridge: [95, 3.0, 15.0, 2.5],
  banana: [89, 1.1, 23.0, 0.3],
  apple: [52, 0.3, 14.0, 0.2],
  "scrambled eggs": [148, 10.0, 1.6, 11.0],
  "avocado toast": [223, 6.0, 24.0, 12.0],
  "beef burger": [244, 15.0, 20.0, 12.0],
  fries: [312, 3.4, 41.0, 15.0],
  salmon: [208, 20.0, 0.0, 13.0],
  "greek yogurt": [59, 10.0, 3.6, 0.4],
};

const ALIASES: Record<string, string> = {
  "chicken breast": "grilled chicken breast",
  "chicken thigh": "grilled chicken thigh",
  rice: "white rice",
  curry: "chicken curry",
  pizza: "margherita pizza",
  eggs: "scrambled eggs",
  burger: "beef burger",
  chips: "fries",
  oatmeal: "porridge",
  yogurt: "greek yogurt",
  salad: "mixed salad",
};

function canonical(food: string): string | null {
  const q = food.trim().toLowerCase();
  if (q in TABLE) return q;
  if (q in ALIASES) return ALIASES[q];
  for (const key of Object.keys(TABLE)) {
    if (key.includes(q) || q.includes(key)) return key;
  }
  for (const [alias, key] of Object.entries(ALIASES)) {
    if (alias.includes(q) || q.includes(alias)) return key;
  }
  return null;
}

const round1 = (v: number) => Math.round(v * 10) / 10;

export function lookup(food: string, grams: number): NutritionRow {
  const key = canonical(food);
  if (key === null) {
    return {
      food,
      matched: false,
      grams,
      kcal: Math.round((150 * grams) / 100),
      protein: 0,
      carbs: 0,
      fat: 0,
    };
  }
  const [kcal, p, c, f] = TABLE[key];
  const factor = grams / 100;
  return {
    food: key,
    matched: true,
    grams,
    kcal: Math.round(kcal * factor),
    protein: round1(p * factor),
    carbs: round1(c * factor),
    fat: round1(f * factor),
  };
}
