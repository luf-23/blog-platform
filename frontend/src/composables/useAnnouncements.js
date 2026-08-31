import { ref } from 'vue'
import { getAnnouncementService } from '../api/admin.js'

const hasNewAnnouncements = ref(false)
const newestAnnouncementId = ref(0)
const activeUserKey = ref('')

function storageKey(userKey) {
  return `bp-announcement-last-seen-${userKey}`
}

function getLastSeen(userKey) {
  return Number(localStorage.getItem(storageKey(userKey)) || 0)
}

export async function checkNewAnnouncements(userKey) {
  if (!userKey) {
    activeUserKey.value = ''
    newestAnnouncementId.value = 0
    hasNewAnnouncements.value = false
    return
  }
  activeUserKey.value = String(userKey)
  const lastSeenId = getLastSeen(activeUserKey.value)
  const result = await getAnnouncementService(lastSeenId)
  const incoming = result.data || []
  newestAnnouncementId.value = incoming.reduce((max, item) => Math.max(max, Number(item.id || 0)), lastSeenId)
  hasNewAnnouncements.value = incoming.some(item => Number(item.id || 0) > lastSeenId)
}

export function markAnnouncementsSeen(userKey = activeUserKey.value, explicitId) {
  if (!userKey) return
  const id = Number(explicitId || newestAnnouncementId.value || getLastSeen(userKey))
  localStorage.setItem(storageKey(userKey), String(id))
  newestAnnouncementId.value = id
  hasNewAnnouncements.value = false
}

export function useAnnouncements() {
  return { hasNewAnnouncements, newestAnnouncementId, checkNewAnnouncements, markAnnouncementsSeen }
}
