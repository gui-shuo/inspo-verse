<script setup lang="ts">
import { ref, onMounted } from 'vue'
import GlitchText from '@/components/ui/GlitchText.vue'
import { Calendar, Tv, PlayCircle, Clock, Bell, X, Star, Volume2, VolumeX, Image as ImageIcon } from 'lucide-vue-next'

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 自动匹配当前星期 (0=Sunday, 1=Monday... 需转换为 0=Monday, 6=Sunday)
const today = new Date().getDay()
const activeDay = ref((today + 6) % 7)

const selectedAnime = ref<any>(null)

const openDetail = (anime: any) => {
  selectedAnime.value = anime
}

const closeDetail = () => {
  selectedAnime.value = null
}

// 稳定图源：Steam CDN (Header + Library Hero)
// 使用 library_hero.jpg 作为详情页顶图，分辨率高且适配宽屏，加载稳定无 404
const schedule: Record<number, any[]> = {
  0: [ // Monday
    { 
      title: '关于我转生变成史莱姆这档事 第三季', 
      time: '23:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1696630/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1696630/library_hero.jpg',
      episode: '第 58 话', 
      status: '更新中',
      score: 9.2,
      description: '利姆路击败克雷曼，正式成为魔王。在魔物之国“鸠拉·特恩佩斯特”联邦国，虽然有短暂的和平，但新的阴谋正在逼近。圣骑士团的威胁、西方的神圣法皇国坂口日向的动向……为了守护伙伴，利姆路将面临新的挑战。'
    }, 
    { 
      title: '狼与香辛料 MERCHANT MEETS THE WISE WOLF', 
      time: '01:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1065970/header.jpg', 
      hero: 'https://images.pexels.com/photos/2882566/pexels-photo-2882566.jpeg?auto=compress&cs=tinysrgb&w=1920', // 金色麦田，完美契合“丰收之神”主题
      episode: '第 08 话', 
      status: '更新中',
      score: 8.8,
      description: '旅行商人罗伦斯在装满麦子的马车上发现了一位熟睡的少女。她长着狼的耳朵和尾巴，自称是掌管丰收的贤狼赫萝。为了回到遥远的北方故乡，赫萝与罗伦斯踏上了漫长的旅途。'
    },
    { 
      title: '无职转生 II ～到了异世界就拿出真本事～', 
      time: '23:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/740130/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/740130/library_hero.jpg',
      episode: '第 19 话', 
      status: '更新中',
      score: 9.0,
      description: '鲁迪乌斯为了寻找母亲塞妮丝，进入了拉诺亚魔法大学。在这里，他与昔日的青梅竹马希露菲叶特重逢，并结识了许多新朋友。然而，平静的校园生活背后，转移事件的真相正在逐渐浮出水面。'
    }
  ],
  1: [ // Tuesday
    { 
      title: '电锯人', 
      time: '00:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/775500/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/775500/library_hero.jpg',
      episode: '全 12 话', 
      status: '已完结',
      score: 8.9,
      description: '背负着巨额债务的少年淀治，与恶魔波奇塔一起作为恶魔猎人勉强维生。某天，他被信任的人背叛并杀害。在意识模糊中，他与波奇塔立下契约，复活成为拥有电锯心脏的“电锯人”。'
    },
    { 
      title: '孤独摇滚！', 
      time: '00:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1817230/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1817230/library_hero.jpg',
      episode: '全 12 话', 
      status: '已完结',
      score: 9.8,
      description: '绰号“波奇”的后藤一里，是一名喜爱吉他的孤独少女。虽然她在网络上以“吉他英雄”的名义活跃，但在现实中却因严重的社交恐惧症而交不到朋友。某天，她被伊地知虹夏邀请加入了“结束乐队”，从而开始了她的乐队生活。'
    },
    { 
      title: '冰海战记 第二季', 
      time: '00:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/821290/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/821290/library_hero.jpg',
      episode: '全 24 话', 
      status: '已完结',
      score: 9.5,
      description: '托尔芬失去了复仇的目标，沦为奴隶。在农场里，他遇到了同样身为奴隶的艾纳尔。通过与艾纳尔的交流，以及在农场的劳作，托尔芬开始重新审视自己的人生，寻找“真正的战士”的意义。'
    }
  ],
  2: [ // Wednesday
    { 
      title: '【我推的孩子】', 
      time: '23:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1761390/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1761390/library_hero.jpg',
      episode: '第 11 话', 
      status: '更新中',
      score: 9.1,
      description: '在演艺圈这个充满谎言的世界里，由于某种原因，妇产科医生五郎转生成为了他推崇的偶像星野爱的孩子——阿奎亚。为了查明母亲死亡的真相，阿奎亚投身演艺圈，展开了一场关于爱与复仇的故事。'
    },
    { 
      title: '欢迎来到实力至上主义的教室', 
      time: '22:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1687000/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1687000/library_hero.jpg',
      episode: '第 13 话', 
      status: '更新中',
      score: 8.5,
      description: '东京都高度育成高等学校，是一所标榜实力至上、升学率就业率100%的名门高中。然而，这里实际上是一个残酷的竞争社会。绫小路清隆被分配到了被视为“次品”的D班，他将如何利用自己的智慧和手段，在这个弱肉强食的校园中生存下去？'
    },
    { 
      title: '想要成为影之实力者！', 
      time: '22:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1798010/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1798010/library_hero.jpg',
      episode: '全 20 话', 
      status: '已完结',
      score: 8.7,
      description: '少年席德憧憬着以路人身份隐藏实力、在暗中介入事件的“影之实力者”。转生到异世界后，他建立了自己的组织“暗影庭园”，并以此为乐。然而，他随口编造的“黑暗教团”竟然真实存在，而他的组织也在不知不觉中成为了对抗邪恶的真实力量。'
    }
  ],
  3: [ // Thursday
    { 
      title: '迷宫饭', 
      time: '22:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/799640/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/799640/library_hero.jpg',
      episode: '第 21 话', 
      status: '更新中',
      score: 9.4,
      description: '为了拯救被红龙吞下的妹妹，莱欧斯一行人决定重返迷宫深处。由于缺乏资金购买粮草，他们决定——吃掉迷宫里的魔物！史莱姆、蛇怪、宝箱怪……甚至是龙！一场关于迷宫美食的生存冒险就此展开。'
    },
    { 
      title: '摇曳露营△ 第三季', 
      time: '23:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1514360/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1514360/library_hero.jpg',
      episode: '第 07 话', 
      status: '更新中',
      score: 9.6,
      description: '这是关于一群喜爱露营的女高中生的日常故事。抚子、凛以及野外活动社的成员们，继续着她们的露营生活。在富士山脚下，在湖畔，在星空下，享受着美食、美景和彼此的陪伴。'
    },
    { 
      title: '物理魔法使马修', 
      time: '23:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/990080/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/990080/library_hero.jpg',
      episode: '第 12 话', 
      status: '已完结',
      score: 8.0,
      description: '在这个魔法即是一切的世界，少年马修却无法使用魔法。为了守护家人，他锻炼出了一身惊人的肌肉，并进入了魔法学校。他要用千锤百炼的肉体力量，粉碎一切魔法常识！'
    }
  ],
  4: [ // Friday
    { 
      title: '葬送的芙莉莲', 
      time: '23:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/881020/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/881020/library_hero.jpg',
      episode: '第 28 话', 
      status: '已完结',
      score: 9.9,
      description: '勇者一行人打倒魔王后，精灵魔法使芙莉莲开始了独自的旅程。作为长寿种，她目送了人类伙伴的离世。为了更了解人类，也为了完成昔日的约定，她带着新伙伴踏上了前往灵魂长眠之地的旅途。'
    },
    { 
      title: 'JOJO的奇妙冒险 石之海', 
      time: '00:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1372110/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1372110/library_hero.jpg',
      episode: '全 38 话', 
      status: '已完结',
      score: 9.3,
      description: '空条徐伦因被陷害入狱，被关进了“水族馆”监狱。为了救出父亲空条承太郎，并阻止神父普奇的阴谋，徐伦觉醒了替身能力，与伙伴们一起展开了绝地反击。'
    },
    { 
      title: '魔女与野兽', 
      time: '01:28', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/460790/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/460790/library_hero.jpg',
      episode: '第 12 话', 
      status: '已完结',
      score: 8.2,
      description: '为了解开施加在自己身上的诅咒，少女基德与背负着棺材的男子阿沙夫在世界各地寻找魔女。这是一部充满黑暗、暴力与复仇色彩的奇幻冒险故事。'
    }
  ],
  5: [ // Saturday (High Traffic)
    { 
      title: '鬼灭之刃：柱训练篇', 
      time: '23:15', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1490890/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1490890/library_hero.jpg',
      episode: '第 04 话', 
      status: '更新中',
      score: 8.8,
      description: '为了应对即将到来的无限城决战，鬼杀队展开了全员强化的“柱训练”。炭治郎在各位柱的指导下，不断突破极限。而在此期间，鬼舞辻无惨也在暗中寻找祢豆子的下落。'
    },
    { 
      title: '怪兽 8 号', 
      time: '23:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1546400/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1546400/library_hero.jpg',
      episode: '第 08 话', 
      status: '更新中',
      score: 8.4,
      description: '在一个怪兽频繁出现的日本，日比野卡夫卡曾梦想成为防卫队员，却在现实的打击下成为了怪兽清理公司的职员。某天，他被神秘生物寄生，获得了变身怪兽的能力。拥有了“怪兽8号”力量的他，决定再次追逐梦想。'
    },
    { 
      title: '我的英雄学院 第七季', 
      time: '17:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1667100/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1667100/library_hero.jpg',
      episode: '第 05 话', 
      status: '更新中',
      score: 8.6,
      description: '英雄与敌人的最终决战一触即发。绿谷出久与死柄木吊的宿命对决，雄英高中学生们的成长与觉悟。为了守护和平的未来，英雄们将献出一切。'
    },
    { 
      title: '间谍过家家 CODE: White', 
      time: '20:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/2655020/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/2655020/library_hero.jpg',
      episode: '剧场版', 
      status: '热映中',
      score: 9.0,
      description: '为了完成任务，间谍“黄昏”组建了虚假的家庭。而在这次的家庭旅行中，女儿阿尼亚误食了关乎世界和平的巧克力，导致佛杰一家卷入了一场巨大的阴谋之中。'
    }
  ],
  6: [ // Sunday
    { 
      title: '海贼王', 
      time: '09:30', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1089090/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1089090/library_hero.jpg',
      episode: '第 1106 话', 
      status: '长篇连载',
      score: 9.7,
      description: '为了寻找传说中的宝藏“ONE PIECE”，路飞与伙伴们扬帆起航，驶向伟大的航路。蛋头岛篇章开启，草帽一伙将面临贝加庞克的科技与世界政府的追杀。'
    },
    { 
      title: '死神 少爷与黑女仆', 
      time: '22:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1201240/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1201240/library_hero.jpg',
      episode: '第 08 话', 
      status: '更新中',
      score: 8.3,
      description: '被魔女施了“触碰生物就会令其死亡”诅咒的少爷，与忠心耿耿的女仆爱丽丝之间既纯情又危险的恋爱故事。最终章，诅咒的真相即将揭晓。'
    },
    { 
      title: '蓝色监狱', 
      time: '17:00', 
      image: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1163550/header.jpg', 
      hero: 'https://cdn.cloudflare.steamstatic.com/steam/apps/1163550/library_hero.jpg',
      episode: '第 24 话', 
      status: '已完结',
      score: 8.8,
      description: '为了培养出能带领日本队夺得世界杯冠军的“最强前锋”，日本足球联合会实施了“蓝色监狱”计划。300 名高中生前锋被关在一起，进行残酷的淘汰赛。为了生存，必须吞噬他人。'
    }
  ]
}
const danmaku = [
  '前方高能！！！', '泪目了 T_T', '这个作画绝了', '燃烧经费啊', '终于等到更新了', 'AWSL', '这波配合无敌', 'bgm好评', '颜艺满分', '在这里养生？', '这也太帅了吧'
]
</script>

<template>
  <div class="container mx-auto px-4 py-20 min-h-screen relative">
    <!-- Header -->
    <div class="text-center mb-12 relative z-10">
      <h1 class="text-5xl md:text-7xl font-black mb-4"><GlitchText text="ANIME ON-AIR" /></h1>
      <p class="text-pink-400 font-bold tracking-widest">新番导视 · 实时追番</p>
    </div>

    <!-- Week Timeline -->
    <div class="flex justify-center mb-16 sticky top-20 z-30">
      <div class="flex bg-slate-900/80 backdrop-blur-lg border border-white/10 p-1 rounded-2xl shadow-2xl overflow-x-auto max-w-full">
        <button 
          v-for="(day, index) in weekDays" 
          :key="index"
          @click="activeDay = index"
          class="px-6 py-3 rounded-xl font-bold transition-all whitespace-nowrap text-sm md:text-base relative overflow-hidden"
          :class="activeDay === index ? 'text-white shadow-lg' : 'text-gray-500 hover:text-gray-300'"
        >
          <div v-if="activeDay === index" class="absolute inset-0 bg-gradient-to-r from-pink-500 to-purple-600 -z-10"></div>
          {{ day }}
        </button>
      </div>
    </div>

    <!-- Anime List -->
    <div class="grid grid-cols-1 gap-8 max-w-5xl mx-auto relative z-10">
      <transition-group name="list">
        <div 
          v-for="(anime, index) in (schedule[activeDay] || [])" 
          :key="anime.title"
          @click="openDetail(anime)"
          class="group relative bg-slate-800 rounded-3xl overflow-hidden border border-white/5 hover:border-pink-500/50 transition-all duration-500 hover:-translate-y-1 shadow-xl cursor-pointer"
        >
          <!-- Background Blur Effect -->
          <div class="absolute inset-0 opacity-0 group-hover:opacity-20 transition-opacity duration-700 pointer-events-none">
            <img :src="anime.image" class="w-full h-full object-cover blur-2xl" />
          </div>

          <div class="flex flex-col md:flex-row">
            <!-- Cover Image -->
            <div class="md:w-56 h-64 md:h-auto relative overflow-hidden shrink-0">
              <img :src="anime.image" class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
              <!-- Status Tag -->
              <div class="absolute top-0 left-0 px-3 py-1 bg-black/60 backdrop-blur text-xs font-bold text-white rounded-br-lg border-b border-r border-white/10">
                {{ anime.time }} 更新
              </div>
            </div>
            
            <!-- Content -->
            <div class="p-6 md:p-8 flex-1 flex flex-col justify-between relative overflow-hidden">
              <!-- Mock Danmaku -->
              <div class="absolute top-4 right-0 w-full opacity-10 pointer-events-none select-none overflow-hidden">
                 <div class="whitespace-nowrap animate-marquee text-white text-sm font-mono">{{ danmaku[index % danmaku.length] }}   {{ danmaku[(index + 3) % danmaku.length] }}</div>
              </div>

              <div>
                <div class="flex justify-between items-start mb-2">
                  <h3 class="text-2xl font-bold text-white group-hover:text-pink-400 transition-colors line-clamp-1">{{ anime.title }}</h3>
                  <span 
                    class="px-3 py-1 rounded-full text-xs font-bold border"
                    :class="anime.status === '更新中' ? 'bg-green-500/10 text-green-400 border-green-500/30' : 'bg-gray-700/50 text-gray-400 border-gray-600'"
                  >
                    {{ anime.status }}
                  </span>
                </div>
                <p class="text-gray-400 flex items-center gap-2 mt-2">
                  <Tv class="w-4 h-4 text-pink-500" /> 
                  当前进度: <span class="text-white font-bold">{{ anime.episode }}</span>
                </p>
                <p class="text-gray-500 text-sm mt-4 line-clamp-2 pr-8">{{ anime.description }}</p>
              </div>
              
              <div class="mt-8 flex gap-4">
                <button class="flex-1 bg-gradient-to-r from-pink-500 to-purple-600 text-white py-3 rounded-xl font-bold shadow-lg hover:shadow-pink-500/30 transition-all flex items-center justify-center gap-2 group/btn">
                  <PlayCircle class="w-5 h-5 group-hover/btn:scale-110 transition-transform" /> 立即追番
                </button>
                <button class="px-4 bg-slate-700/50 border border-white/10 hover:bg-white/10 text-white rounded-xl transition-colors">
                  <Bell class="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </transition-group>
      
      <div v-if="!schedule[activeDay]" class="text-center py-20 text-gray-500">
        <Calendar class="w-16 h-16 mx-auto mb-4 opacity-20" />
        <p>今日暂无更新，去补补旧番吧~</p>
      </div>
    </div>

    <!-- Anime Detail Modal -->
    <transition name="modal">
      <div v-if="selectedAnime" class="fixed inset-0 z-[100] flex items-center justify-center p-4">
        <div class="absolute inset-0 bg-black/90 backdrop-blur-sm" @click="closeDetail"></div>
        
        <div class="relative w-full max-w-4xl bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-white/10 animate__animated animate__zoomIn flex flex-col max-h-[90vh]">
          
          <button @click="closeDetail" class="absolute top-4 right-4 z-50 p-2 bg-black/50 text-white rounded-full hover:bg-black/80 transition-colors border border-white/10">
            <X class="w-6 h-6" />
          </button>

          <!-- Top: Hero Image -->
          <div class="w-full h-56 md:h-80 relative bg-black group">
            <img 
              :src="selectedAnime.hero || selectedAnime.image" 
              class="w-full h-full object-cover object-top" 
            />
            <div class="absolute inset-0 bg-gradient-to-t from-slate-900 via-transparent to-transparent"></div>
          </div>

          <!-- Bottom: Info -->
          <div class="p-8 md:p-10 overflow-y-auto custom-scrollbar bg-slate-900 flex-1">
            <div class="flex items-center justify-between mb-2">
              <span class="px-3 py-1 bg-pink-500/10 text-pink-400 border border-pink-500/20 rounded text-xs font-bold">{{ selectedAnime.time }} 更新</span>
              <div class="flex items-center gap-1 text-yellow-400 font-bold text-lg">
                <Star class="w-5 h-5 fill-yellow-400" /> {{ selectedAnime.score }}
              </div>
            </div>

            <h2 class="text-3xl md:text-4xl font-black text-white mb-6 leading-tight">{{ selectedAnime.title }}</h2>
            
            <p class="text-gray-300 leading-relaxed mb-8 text-lg">{{ selectedAnime.description }}</p>

            <div class="flex gap-4 mt-auto pt-4 border-t border-white/5">
              <button class="flex-1 py-4 bg-gradient-to-r from-pink-500 to-purple-600 text-white font-bold rounded-xl hover:shadow-[0_0_20px_rgba(236,72,153,0.4)] transition-all flex items-center justify-center gap-2 text-lg">
                <PlayCircle class="w-6 h-6 fill-current" /> 开始观看
              </button>
              <button class="px-6 py-4 bg-slate-800 text-white font-bold rounded-xl hover:bg-slate-700 transition-all border border-white/10 flex items-center gap-2">
                <Bell class="w-5 h-5" /> 追番
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.animate-marquee {
  animation: marquee 15s linear infinite;
}
@keyframes marquee {
  from { transform: translateX(100%); }
  to { transform: translateX(-100%); }
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(30px);
}
</style>
