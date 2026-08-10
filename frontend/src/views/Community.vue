<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '../store/token.js'
import { useUserInfoStore } from '../store/userInfo.js'
import {
  createCommunityPostService,
  getCommunityFeedService,
  getFollowingCommunityFeedService,
  getCommunityMetaService,
  toggleCommunityFollowService,
  toggleCommunityLikeService,
  voteCommunityPollService
} from '../api/community.js'

const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const posts = ref([])
const meta = reactive({ hotTopics: [], recommendedCreators: [] })
const loading = ref(false)
const publishing = ref(false)
const composerOpen = ref(false)
const selectedSort = ref('hot')
const selectedTopic = ref('全部')
const likedPosts = reactive(new Set())
const followedUsers = reactive(new Set())
const votedPosts = reactive(new Set())

const form = reactive({
  type: 'share',
  title: '',
  content: '',
  topic: '技术开发',
  options: ['深度长文 / 教程', '实战案例 / 经验分享']
})

const topics = ['全部', '技术开发', '产品设计', '写作成长', 'AI 探索', '副业与成长']
const sortTabs = [
  { label: '关注', value: 'following' },
  { label: '推荐', value: 'hot' },
  { label: '最新', value: 'latest' }
]
const typeInfo = {
  question: { label: '提问', class: 'question', icon: '?' },
  share: { label: '分享', class: 'share', icon: '✦' },
  poll: { label: '讨论', class: 'poll', icon: '▥' }
}
const fallbackAvatars = ['/avatar/avatar1.png', '/avatar/avatar2.png', '/avatar/avatar3.jpg']
const sampleCode = "// 快捷键：Cmd/Ctrl + Shift + M 预览\neditor.setOption('lineNumbers', true)\neditor.setOption('theme', 'material')"

const currentAvatar = computed(() => userInfoStore.userInfo?.avatarImage || '/avatar/avatar1.png')

function requireLogin() {
  if (tokenStore.token) return true
  router.push({ path: '/login', query: { redirect: '/community' } })
  return false
}

async function fetchFeed() {
  loading.value = true
  try {
    const service = selectedSort.value === 'following'
      ? getFollowingCommunityFeedService
      : getCommunityFeedService
    const result = await service({
      sort: selectedSort.value,
      topic: selectedTopic.value === '全部' ? undefined : selectedTopic.value,
      page: 1,
      pageSize: 20
    })
    posts.value = result.data?.list || []
  } finally {
    loading.value = false
  }
}

async function fetchMeta() {
  const result = await getCommunityMetaService()
  meta.hotTopics = result.data?.hotTopics || []
  meta.recommendedCreators = result.data?.recommendedCreators || []
}

function setSort(value) {
  if (value === 'following' && !requireLogin()) return
  selectedSort.value = value
  fetchFeed()
}

function setTopic(topic) {
  selectedTopic.value = topic
  fetchFeed()
}

function openComposer(type = 'share') {
  if (!requireLogin()) return
  form.type = type
  composerOpen.value = true
}

function resetForm() {
  Object.assign(form, { type: 'share', title: '', content: '', topic: '技术开发', options: ['', ''] })
}

async function publish() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和正文')
    return
  }
  if (form.type === 'poll' && form.options.filter(Boolean).length < 2) {
    ElMessage.warning('投票至少需要两个选项')
    return
  }
  publishing.value = true
  try {
    await createCommunityPostService({ ...form, options: form.options.filter(Boolean) })
    ElMessage.success('讨论已发布')
    composerOpen.value = false
    resetForm()
    await Promise.all([fetchFeed(), fetchMeta()])
  } finally {
    publishing.value = false
  }
}

async function toggleLike(post) {
  if (!requireLogin()) return
  const result = await toggleCommunityLikeService(post.postId)
  const liked = Boolean(result.data?.liked)
  if (liked) likedPosts.add(post.postId)
  else likedPosts.delete(post.postId)
  post.likeCount = Math.max(0, (post.likeCount || 0) + (liked ? 1 : -1))
}

async function vote(post, option) {
  if (!requireLogin() || votedPosts.has(post.postId)) return
  await voteCommunityPollService(post.postId, option.optionId)
  votedPosts.add(post.postId)
  option.voteCount = (option.voteCount || 0) + 1
  post.voteCount = (post.voteCount || 0) + 1
  ElMessage.success('投票成功，感谢参与')
}

async function toggleFollow(creator) {
  if (!requireLogin()) return
  const id = valueOf(creator, 'userId', 'user_id')
  const result = await toggleCommunityFollowService(id)
  if (result.data?.following) followedUsers.add(id)
  else followedUsers.delete(id)
}

function addOption() {
  if (form.options.length < 6) form.options.push('')
}

function removeOption(index) {
  if (form.options.length > 2) form.options.splice(index, 1)
}

function formatTime(value) {
  if (!value) return '刚刚'
  const diff = Date.now() - new Date(value).getTime()
  if (diff < 3600000) return Math.max(1, Math.floor(diff / 60000)) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + ' 天前'
  return new Date(value).toLocaleDateString('zh-CN')
}

function formatCount(value) {
  const count = Number(value || 0)
  if (count >= 10000) return (count / 10000).toFixed(1) + '万'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return String(count)
}

function percentage(post, option) {
  if (!post.voteCount) return 0
  return Math.round((option.voteCount || 0) / post.voteCount * 100)
}

function valueOf(object, camel, snake) {
  return object?.[camel] ?? object?.[snake]
}

onMounted(() => Promise.all([fetchFeed(), fetchMeta()]))
</script>

<template>
  <div class="community-page workspace-page">
    <div class="page-container community-layout workspace-frame">
      <section class="community-main workspace-scroll">
        <header class="community-header">
          <div>
            <p class="eyebrow">CREATOR COMMUNITY</p>
            <h1>社区广场</h1>
            <p>和创作者一起交流、提问与分享</p>
          </div>
          <button class="btn btn-primary publish-button" @click="openComposer('share')">
            <span>＋</span> 发布讨论
          </button>
        </header>

        <button class="composer-card surface-card" @click="openComposer('share')">
          <img :src="currentAvatar" alt="我的头像" />
          <span>分享一个想法或提出问题…</span>
          <span class="composer-tool"><b>▧</b> 图文</span>
          <span class="composer-tool" @click.stop="openComposer('poll')"><b>▥</b> 投票</span>
          <span class="composer-tool"><b>&lt;/&gt;</b> 代码</span>
          <span class="composer-tool"><b>↗</b> 链接</span>
        </button>

        <div class="feed-controls">
          <div class="sort-tabs">
            <button v-for="tab in sortTabs" :key="tab.value" :class="{ active: selectedSort === tab.value }" @click="setSort(tab.value)">{{ tab.label }}</button>
          </div>
          <div class="topic-tabs">
            <button v-for="topic in topics" :key="topic" :class="{ active: selectedTopic === topic }" @click="setTopic(topic)">{{ topic }}</button>
          </div>
        </div>

        <div v-if="loading" class="feed-loading">
          <div v-for="i in 3" :key="i" class="post-skeleton surface-card"><i></i><span></span><span></span><span></span></div>
        </div>

        <div v-else-if="posts.length" class="post-list">
          <article
            v-for="post in posts"
            :key="post.postId"
            class="community-post surface-card"
            :class="'community-post--' + (typeInfo[post.type]?.class || 'share')"
          >
            <aside class="post-kind">
              <span>{{ typeInfo[post.type]?.icon }}</span>
              <strong>{{ typeInfo[post.type]?.label }}</strong>
            </aside>
            <div class="post-content">
              <header class="post-author">
                <img :src="post.authorAvatar || '/avatar/avatar1.png'" :alt="post.authorNickname" />
                <div>
                  <strong>{{ post.authorNickname || post.authorUsername }}</strong>
                  <p>{{ formatTime(post.createTime) }} · {{ post.authorSignature || post.topic }}</p>
                </div>
                <span class="level-badge">Lv.{{ (post.userId || 1) % 5 + 3 }}</span>
                <span v-if="post.solved" class="solved-badge">✓ 已解决</span>
              </header>

              <h2>{{ post.title }}</h2>
              <p class="post-excerpt">{{ post.content }}</p>

              <pre v-if="post.type === 'share' && post.topic === '产品设计'" class="post-code"><code>{{ sampleCode }}</code><button>复制</button></pre>

              <div v-if="post.type === 'poll'" class="poll-options">
                <button v-for="option in post.options" :key="option.optionId" :disabled="votedPosts.has(post.postId)" @click="vote(post, option)">
                  <span class="poll-radio"></span><strong>{{ option.optionText }}</strong><em>{{ percentage(post, option) }}% ({{ option.voteCount }})</em>
                  <i :style="{ width: percentage(post, option) + '%' }"></i>
                </button>
              </div>

              <div class="post-tags">
                <span>{{ post.topic }}</span>
                <span v-if="post.type === 'question'">高并发</span>
                <span v-if="post.type === 'share'">经验分享</span>
              </div>

              <footer class="post-footer">
                <div class="participant-stack">
                  <img v-for="(avatar, index) in fallbackAvatars" :key="avatar" :src="avatar" :style="{ zIndex: 3 - index }" alt="" />
                  <span>{{ post.type === 'poll' ? formatCount(post.voteCount) + ' 人参与投票' : formatCount(post.commentCount + 8) + ' 人参与讨论' }}</span>
                </div>
                <div class="post-actions">
                  <button @click="ElMessage.info('讨论详情将在后续接入评论线程')"><span>▢</span>{{ post.commentCount }}</button>
                  <button :class="{ active: likedPosts.has(post.postId) }" @click="toggleLike(post)"><span>♡</span>{{ post.likeCount }}</button>
                  <button aria-label="收藏"><span>♧</span></button>
                </div>
              </footer>
            </div>
          </article>
        </div>

        <div v-else class="empty-state surface-card"><strong>还没有相关讨论</strong><p>换个话题看看，或者发布第一条内容。</p></div>
      </section>

      <aside class="community-aside workspace-scroll">
        <section class="aside-card surface-card">
          <header><h2>热门话题</h2><button>更多 ›</button></header>
          <button
            v-for="(topic, index) in meta.hotTopics"
            :key="valueOf(topic, 'topic', 'topic')"
            class="hot-topic"
            @click="setTopic(valueOf(topic, 'topic', 'topic'))"
          >
            <span :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <strong>{{ valueOf(topic, 'topic', 'topic') }}</strong>
            <em>{{ formatCount(valueOf(topic, 'participantCount', 'participant_count') || valueOf(topic, 'postCount', 'post_count')) }} 参与讨论</em>
          </button>
        </section>

        <section class="aside-card surface-card">
          <header><h2>值得关注</h2><button>更多 ›</button></header>
          <div v-for="creator in meta.recommendedCreators" :key="valueOf(creator, 'userId', 'user_id')" class="creator-row">
            <img :src="valueOf(creator, 'avatarImage', 'avatar_image') || '/avatar/avatar1.png'" alt="" />
            <div><strong>{{ valueOf(creator, 'nickname', 'nickname') }}</strong><span>{{ valueOf(creator, 'signature', 'signature') || '持续分享优质内容' }}</span></div>
            <button :class="{ active: followedUsers.has(valueOf(creator, 'userId', 'user_id')) }" @click="toggleFollow(creator)">
              {{ followedUsers.has(valueOf(creator, 'userId', 'user_id')) ? '已关注' : '关注' }}
            </button>
          </div>
        </section>

        <section class="aside-card surface-card community-rules">
          <header><h2>社区公约</h2></header>
          <p><i class="rule-blue">♡</i><span><strong>友善尊重</strong>尊重他人观点，友善沟通与讨论。</span></p>
          <p><i class="rule-cyan">▣</i><span><strong>内容真实</strong>分享真实经验，拒绝抄袭与造假。</span></p>
          <p><i class="rule-red">♧</i><span><strong>主题相关</strong>保持讨论聚焦，避免无关内容。</span></p>
          <p><i class="rule-gray">△</i><span><strong>遵守法律</strong>遵守法律法规，不发布违法信息。</span></p>
          <router-link to="/announcement">查看完整公约 ›</router-link>
        </section>
      </aside>
    </div>

    <el-dialog v-model="composerOpen" title="发布社区讨论" width="min(620px, calc(100vw - 28px))" :close-on-click-modal="false">
      <div class="publish-form">
        <div class="publish-types">
          <button v-for="(info, type) in typeInfo" :key="type" :class="[info.class, { active: form.type === type }]" @click="form.type = type"><b>{{ info.icon }}</b>{{ info.label }}</button>
        </div>
        <label><span>标题</span><el-input v-model="form.title" maxlength="160" show-word-limit placeholder="用一句话说明你想讨论的内容" /></label>
        <label><span>正文</span><el-input v-model="form.content" type="textarea" :rows="6" maxlength="2000" show-word-limit placeholder="补充背景、你的思考，或希望大家讨论的问题…" /></label>
        <label><span>话题</span><el-select v-model="form.topic" style="width:100%"><el-option v-for="topic in topics.slice(1)" :key="topic" :label="topic" :value="topic" /></el-select></label>
        <div v-if="form.type === 'poll'" class="poll-editor">
          <span>投票选项</span>
          <div v-for="(_, index) in form.options" :key="index"><el-input v-model="form.options[index]" :placeholder="'选项 ' + (index + 1)" /><button @click="removeOption(index)">×</button></div>
          <button class="add-option" @click="addOption">＋ 添加选项</button>
        </div>
      </div>
      <template #footer><el-button @click="composerOpen = false">取消</el-button><el-button type="primary" :loading="publishing" @click="publish">发布讨论</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.community-layout { display: grid; grid-template-columns: minmax(0, 1fr) 390px; gap: 28px; align-items: stretch; }
.community-main, .community-aside { padding: 26px 4px 48px; }
.community-main { min-width: 0; padding-right: 6px; }
.community-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.community-header h1 { margin: 4px 0 2px; font-family: inherit; font-size: clamp(30px, 3vw, 40px); font-weight: 900; line-height: 1.15; letter-spacing: -.03em; }
.community-header > div > p:last-child { color: var(--c-text-3); font-size: 15px; }
.publish-button span { font-size: 20px; font-weight: 400; }
.composer-card { display: grid; width: 100%; grid-template-columns: auto 1fr repeat(4, auto); align-items: center; gap: 16px; padding: 14px 0; border: 0; border-block: 1px solid var(--c-text); border-radius: 0; background: transparent; box-shadow: none; text-align: left; color: var(--c-text-3); transition: all var(--transition); }
.composer-card:hover { color: var(--c-text); }
.composer-card > img { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; }
.composer-card > span:nth-child(2) { font-size: 15px; }
.composer-tool { display: inline-flex; align-items: center; gap: 6px; color: var(--c-text-3); font-size: 13px; }
.composer-tool b { color: var(--c-text-2); font-size: 14px; }
.feed-controls { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-top: 18px; border-bottom: 1px solid var(--c-border); }
.sort-tabs { display: flex; gap: 12px; }
.sort-tabs button { position: relative; padding: 10px 12px 12px; border: 0; background: transparent; color: var(--c-text-2); font-size: 15px; font-weight: 600; }
.sort-tabs button::after { position: absolute; right: 9px; bottom: -1px; left: 9px; height: 3px; border-radius: 4px 4px 0 0; background: var(--c-primary); content: ''; opacity: 0; }
.sort-tabs button.active { color: var(--c-primary); }
.sort-tabs button.active::after { opacity: 1; }
.topic-tabs { display: flex; gap: 5px; padding-bottom: 7px; overflow-x: auto; }
.topic-tabs button { flex: 0 0 auto; padding: 6px 8px; border: 0; border-bottom: 1px solid transparent; border-radius: 0; background: transparent; color: var(--c-text-3); font-size: 12px; }
.topic-tabs button.active { border-color: var(--c-text); color: var(--c-text); font-weight: 800; }
.post-list { display: flex; flex-direction: column; gap: 10px; padding-top: 4px; }
.community-post { --kind: var(--c-community); display: grid; grid-template-columns: 96px minmax(0, 1fr); overflow: hidden; border: 0; border-bottom: 1px solid var(--c-border-strong); border-left: 3px solid var(--kind); border-radius: 0; box-shadow: none; }
.community-post--share { --kind: var(--c-warning); }
.community-post--poll { --kind: var(--c-primary); }
.post-kind { display: flex; align-items: center; justify-content: flex-start; flex-direction: column; gap: 6px; padding: 22px 12px; border-right: 1px solid var(--c-border); background: color-mix(in srgb, var(--kind) 4%, var(--c-surface)); color: var(--kind); }
.post-kind span { display: grid; width: 48px; height: 48px; place-items: center; border-radius: 12px; background: color-mix(in srgb, var(--kind) 11%, transparent); font-size: 27px; font-weight: 700; }
.post-kind strong { font-size: 13px; }
.post-content { min-width: 0; padding: 17px 20px 15px; }
.post-author { display: flex; align-items: center; gap: 9px; }
.post-author img { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.post-author div { min-width: 0; }
.post-author strong { display: block; color: var(--c-text); font-size: 13px; }
.post-author p { overflow: hidden; color: var(--c-text-4); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.level-badge { padding: 2px 6px; border-radius: 4px; background: var(--c-warning-soft); color: var(--c-warning); font-size: 10px; font-weight: 700; }
.solved-badge { margin-left: auto; padding: 5px 10px; border: 1px solid color-mix(in srgb, var(--c-community) 28%, transparent); border-radius: var(--radius-sm); background: var(--c-community-soft); color: var(--c-community); font-size: 12px; font-weight: 700; }
.post-content h2 { margin: 10px 0 3px; font-size: 18px; line-height: 1.4; }
.post-excerpt { display: -webkit-box; overflow: hidden; color: var(--c-text-2); font-size: 13px; line-height: 1.65; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.post-tags { display: flex; gap: 6px; margin-top: 10px; }
.post-tags span { padding: 3px 8px; border-radius: 4px; background: var(--c-surface-3); color: var(--c-text-3); font-size: 11px; }
.post-code { position: relative; margin: 10px 0 0; padding: 10px 38px 10px 12px; overflow: auto; border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface-2); color: var(--c-text-2); font: 11px/1.55 Consolas, monospace; }
.post-code button { position: absolute; top: 7px; right: 7px; border: 0; background: transparent; color: var(--c-text-4); font-size: 11px; }
.post-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; }
.participant-stack { display: flex; align-items: center; color: var(--c-text-3); font-size: 11px; }
.participant-stack img { width: 22px; height: 22px; margin-left: -6px; border: 2px solid var(--c-surface); border-radius: 50%; object-fit: cover; }
.participant-stack img:first-child { margin-left: 0; }
.participant-stack span { margin-left: 7px; }
.post-actions { display: flex; align-items: center; gap: 7px; }
.post-actions button { display: inline-flex; align-items: center; gap: 4px; padding: 4px 6px; border: 0; border-radius: 6px; background: transparent; color: var(--c-text-3); font-size: 12px; }
.post-actions button:hover, .post-actions button.active { background: var(--c-primary-soft); color: var(--c-primary); }
.post-actions span { font-size: 18px; line-height: 1; }
.poll-options { display: flex; flex-direction: column; gap: 5px; margin-top: 10px; }
.poll-options button { position: relative; display: grid; min-height: 29px; grid-template-columns: auto 1fr auto; align-items: center; gap: 7px; overflow: hidden; padding: 5px 10px; border: 1px solid var(--c-border); border-radius: 5px; background: var(--c-surface); color: var(--c-text-2); text-align: left; }
.poll-options button > * { position: relative; z-index: 1; }
.poll-options button i { position: absolute; z-index: 0; top: 0; bottom: 0; left: 0; background: color-mix(in srgb, var(--c-community) 13%, transparent); transition: width .3s ease; }
.poll-options button em { font-size: 11px; font-style: normal; }
.poll-options button strong { font-size: 11px; font-weight: 600; }
.poll-radio { width: 13px; height: 13px; border: 2px solid var(--c-text-4); border-radius: 50%; }
.community-aside { display: flex; flex-direction: column; gap: 30px; padding-left: 0; }
.aside-card { padding: 16px 0 0; border: 0; border-top: 3px solid var(--c-text); border-radius: 0; background: transparent; box-shadow: none; }
.aside-card > header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.aside-card h2 { font-family: inherit; font-size: 18px; font-weight: 900; }
.aside-card header button { border: 0; background: transparent; color: var(--c-text-3); font-size: 12px; }
.hot-topic { display: grid; width: 100%; grid-template-columns: 24px 1fr auto; align-items: center; gap: 8px; padding: 7px 0; border: 0; background: transparent; text-align: left; }
.hot-topic > span { display: grid; width: 22px; height: 22px; place-items: center; border-radius: 6px; background: var(--c-surface-3); color: var(--c-text-3); font-size: 11px; font-weight: 700; }
.hot-topic > span.top { background: linear-gradient(135deg, #ff6b55, #ff9f43); color: #fff; }
.hot-topic strong { overflow: hidden; color: var(--c-text-2); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.hot-topic em { color: var(--c-text-4); font-size: 10px; font-style: normal; }
.creator-row { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 9px; padding: 8px 0; }
.creator-row img { width: 38px; height: 38px; border-radius: 50%; object-fit: cover; }
.creator-row div { display: flex; min-width: 0; flex-direction: column; }
.creator-row strong { font-size: 13px; }
.creator-row span { overflow: hidden; color: var(--c-text-4); font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.creator-row button { padding: 5px 12px; border: 1px solid var(--c-primary); border-radius: 6px; background: transparent; color: var(--c-primary); font-size: 12px; font-weight: 700; }
.creator-row button.active { border-color: var(--c-border); color: var(--c-text-3); }
.community-rules p { display: flex; gap: 10px; margin: 10px 0; color: var(--c-text-3); font-size: 11px; }
.community-rules i { display: grid; width: 27px; height: 27px; flex: 0 0 auto; place-items: center; border-radius: 50%; font-style: normal; }
.community-rules span { display: flex; flex-direction: column; }
.community-rules strong { color: var(--c-text-2); font-size: 12px; }
.rule-blue { background: var(--c-primary-soft); color: var(--c-primary); }
.rule-cyan { background: var(--c-community-soft); color: var(--c-community); }
.rule-red { background: var(--c-danger-soft); color: var(--c-danger); }
.rule-gray { background: var(--c-surface-3); color: var(--c-text-3); }
.community-rules a { display: block; margin-top: 12px; color: var(--c-primary); font-size: 12px; font-weight: 700; }
.feed-loading { display: flex; flex-direction: column; gap: 10px; padding-top: 10px; }
.post-skeleton { display: flex; min-height: 170px; flex-direction: column; gap: 12px; padding: 24px 110px; }
.post-skeleton i, .post-skeleton span { height: 14px; border-radius: 7px; background: linear-gradient(90deg, var(--c-surface-2), var(--c-surface-3), var(--c-surface-2)); background-size: 200% 100%; animation: shimmer 1.3s infinite; }
.post-skeleton i { width: 160px; }
.post-skeleton span:nth-child(2) { width: 55%; height: 20px; }
.post-skeleton span:nth-child(3) { width: 92%; }
.post-skeleton span:nth-child(4) { width: 70%; }
@keyframes shimmer { to { background-position: -200% 0; } }
.publish-form { display: flex; flex-direction: column; gap: 18px; }
.publish-types { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.publish-types button { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 10px; border: 1px solid var(--c-border); border-radius: var(--radius-sm); background: var(--c-surface-2); color: var(--c-text-2); }
.publish-types button.active { border-color: var(--c-primary); background: var(--c-primary-soft); color: var(--c-primary); }
.publish-form label { display: flex; flex-direction: column; gap: 7px; }
.publish-form label > span, .poll-editor > span { color: var(--c-text-2); font-weight: 700; }
.poll-editor { display: flex; flex-direction: column; gap: 8px; }
.poll-editor > div { display: flex; gap: 7px; }
.poll-editor > div button, .add-option { border: 0; background: transparent; color: var(--c-text-3); }
.add-option { align-self: flex-start; color: var(--c-primary); font-weight: 700; }

@media (max-width: 1100px) { .community-layout { grid-template-columns: minmax(0, 1fr) 340px; } }
@media (max-width: 900px) {
  .community-page { overflow-y: auto; }
  .community-layout { display: block; height: auto; }
  .community-main, .community-aside { overflow: visible; }
  .community-aside { display: grid; grid-template-columns: repeat(2, 1fr); padding-top: 0; }
  .community-rules { grid-column: 1 / -1; }
}
@media (max-width: 760px) {
  .community-header { align-items: flex-start; }
  .composer-card { grid-template-columns: auto 1fr; }
  .composer-tool { display: none; }
  .feed-controls { align-items: stretch; flex-direction: column; }
  .topic-tabs { padding-bottom: 8px; }
  .community-post { grid-template-columns: 1fr; }
  .post-kind { align-items: center; flex-direction: row; padding: 8px 14px; border-right: 0; border-bottom: 1px solid var(--c-border); }
  .post-kind span { width: 28px; height: 28px; font-size: 16px; }
  .community-aside { grid-template-columns: 1fr; }
  .community-rules { grid-column: auto; }
}
@media (max-width: 520px) {
  .community-header .publish-button { width: 40px; min-width: 40px; padding: 0; font-size: 0; }
  .community-header .publish-button span { font-size: 21px; }
  .post-content { padding: 14px; }
  .post-footer { align-items: flex-start; flex-direction: column; }
  .publish-types { grid-template-columns: 1fr; }
}

/* Developer community workspace */
.community-header h1 { font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; font-size: clamp(28px, 3vw, 36px); font-weight: 850; }
.composer-card { padding: 14px 18px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.composer-card:hover { border-color: #bfdbfe; box-shadow: var(--shadow-sm); }
.topic-tabs button { padding: 6px 11px; border: 1px solid transparent; border-radius: 5px; background: var(--c-surface-2); }
.topic-tabs button.active { border-color: var(--c-primary); background: var(--c-primary); color: #fff; }
.community-post { border: 1px solid var(--c-border); border-left: 3px solid var(--kind); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.community-aside { gap: 14px; }
.aside-card { padding: 20px; border: 1px solid var(--c-border); border-radius: var(--radius-lg); background: var(--c-surface); box-shadow: var(--shadow-xs); }
.aside-card h2 { font-family: Inter, "PingFang SC", sans-serif; font-size: 16px; font-weight: 800; }
</style>
