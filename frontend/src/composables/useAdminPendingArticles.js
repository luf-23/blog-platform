import { ref } from 'vue'
import { getAdminStatsService } from '../api/admin.js'

const pendingArticles = ref(0)
let refreshVersion = 0

async function refreshPendingArticles() {
  const version = ++refreshVersion
  const res = await getAdminStatsService()
  const nextCount = Number(res.data?.pendingArticles || 0)

  if (version === refreshVersion) {
    pendingArticles.value = nextCount
  }

  return nextCount
}

export function useAdminPendingArticles() {
  return {
    pendingArticles,
    refreshPendingArticles
  }
}
