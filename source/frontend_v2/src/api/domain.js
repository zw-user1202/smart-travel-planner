import api from '@/api'

export const RECOMMENDED_ROUTES = [
  { title: '长沙三日 · 美食人文', dest: '长沙', days: 3, prefs: '人文历史,美食探店', budget: 2400 },
  { title: '成都慢生活四日', dest: '成都', days: 4, prefs: '美食探店,休闲放松', budget: 3200 },
  { title: '杭州西湖休闲三日', dest: '杭州', days: 3, prefs: '自然风光,休闲放松', budget: 2600 },
  { title: '厦门海岛四日', dest: '厦门', days: 4, prefs: '自然风光,摄影打卡', budget: 3500 },
  { title: '西安古都三日', dest: '西安', days: 3, prefs: '人文历史,摄影打卡', budget: 2500 },
  { title: '大理丽江五日', dest: '大理', days: 5, prefs: '自然风光,户外徒步', budget: 4200 }
]

export function parsePlan(trip) {
  if (!trip?.planJson) return null
  try {
    return JSON.parse(trip.planJson)
  } catch {
    return null
  }
}

export function aggregateCosts(trips) {
  const byType = {}
  let total = 0
  let budget = 0
  let estimated = 0
  for (const t of trips || []) {
    if (t.budget) budget += Number(t.budget)
    if (t.estimatedCost) estimated += Number(t.estimatedCost)
    const plan = parsePlan(t)
    if (!plan?.days) continue
    for (const day of plan.days) {
      for (const item of day.items || []) {
        const cost = Number(item.cost || 0)
        if (!cost) continue
        const type = item.type || '其他'
        byType[type] = (byType[type] || 0) + cost
        total += cost
      }
    }
  }
  const list = Object.entries(byType)
    .map(([type, cost]) => ({ type, cost, pct: total ? Math.round((cost / total) * 100) : 0 }))
    .sort((a, b) => b.cost - a.cost)
  return { byType: list, total, budget, estimated }
}

export function footprintStats(trips) {
  const cityCount = {}
  let days = 0
  let done = 0
  for (const t of trips || []) {
    if (t.destination) cityCount[t.destination] = (cityCount[t.destination] || 0) + 1
    if (t.startDate && t.endDate) {
      const d = (new Date(t.endDate) - new Date(t.startDate)) / 86400000 + 1
      if (d > 0 && d < 400) days += d
    }
    if (t.status === 'done') done += 1
  }
  const cities = Object.entries(cityCount)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
  return {
    cities: cities.length,
    days,
    trips: (trips || []).length,
    done,
    cityList: cities
  }
}

export async function loadAllTrips() {
  return (await api.get('/trips')) || []
}

const CHECK_KEY = 'travel_checklist_v1'
const DEFAULT_CHECKS = [
  { id: 'c1', group: '证件票据', text: '身份证 / 护照', done: false },
  { id: 'c2', group: '证件票据', text: '车票 / 机票订单', done: false },
  { id: 'c3', group: '证件票据', text: '酒店预订确认', done: false },
  { id: 'c4', group: '衣物洗护', text: '换洗衣物', done: false },
  { id: 'c5', group: '衣物洗护', text: '洗漱用品', done: false },
  { id: 'c6', group: '电子设备', text: '充电器 / 充电宝', done: false },
  { id: 'c7', group: '电子设备', text: '耳机', done: false },
  { id: 'c8', group: '出行确认', text: '查看目的地天气', done: false },
  { id: 'c9', group: '出行确认', text: '热门景点预约', done: false },
  { id: 'c10', group: '出行确认', text: '确认返程班次', done: false }
]

export function loadChecklist() {
  try {
    const raw = localStorage.getItem(CHECK_KEY)
    if (raw) return JSON.parse(raw)
  } catch {}
  return DEFAULT_CHECKS.map(x => ({ ...x }))
}

export function saveChecklist(list) {
  localStorage.setItem(CHECK_KEY, JSON.stringify(list))
}

export { DEFAULT_CHECKS, CHECK_KEY }
