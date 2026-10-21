/* ============================================================
 * 机器视图物理组件:全部无自身状态,数据由根组件下发,事件向上抛
 * 对应物理部件:转子鼓 / 灯板 / 键盘 / 插线板 / 信号路径
 * ============================================================ */
window.EnigmaShared = {
  /* 德式 QWERTZ 键盘三排布局(历史机型) */
  KEY_ROWS: ['QWERTZUIO', 'ASDFGHJK', 'PYXCVBNML'],
  /* 转子缺口对照(与 Java 端 RotorType 一致) */
  NOTCH: { I: 'Q', II: 'E', III: 'V', IV: 'J', V: 'Z' }
};

function registerComponents(app) {

  /* 单个转子鼓:窗口三格字母,当前格高亮,缺口对准时显示标记 */
  app.component('rotor-drum', {
    props: { label: String, name: String, ring: String, notch: String, letters: Array },
    template: `
      <div class="drum">
        <div class="name">{{ label }}转子 {{ name }}</div>
        <div class="window"><div class="strip">
          <div class="cell">{{ letters[0] }}</div>
          <div class="cell cur">{{ letters[1] }}</div>
          <div class="cell">{{ letters[2] }}</div>
        </div></div>
        <div class="notch-mark">{{ letters[1] === notch ? '◀ 缺口' : '' }}</div>
        <div class="rotor-pos">环 {{ ring }} · 缺口 {{ notch }}</div>
      </div>`
  });

  /* 灯板:26 灯三排, lit 为当前亮起的字母(450ms 后由根组件熄灭) */
  app.component('lamp-board', {
    props: { lit: { type: String, default: '' } },
    computed: {
      rows() { return window.EnigmaShared.KEY_ROWS.map((r) => r.split('')); }
    },
    template: `
      <div class="lamps">
        <div class="row" v-for="row in rows" :key="row[0]">
          <div v-for="ch in row" :key="ch" class="lamp" :class="{ on: lit === ch }">{{ ch }}</div>
        </div>
      </div>`
  });

  /* 键盘:三排 QWERTZ,敲击事件上抛 */
  app.component('key-board', {
    emits: ['press'],
    computed: {
      rows() { return window.EnigmaShared.KEY_ROWS.map((r) => r.split('')); }
    },
    template: `
      <div class="keys">
        <div class="row" v-for="row in rows" :key="row[0]">
          <button v-for="ch in row" :key="ch" class="key" @click="$emit('press', ch)">{{ ch }}</button>
        </div>
      </div>`
  });

  /* 插线板:26 孔,成对高亮;点两孔插一对,点已配对孔拔线 */
  app.component('plug-board', {
    props: { pairs: { type: Array, default: () => [] }, pending: { type: String, default: '' } },
    emits: ['toggle'],
    computed: {
      sockets() {
        const out = [];
        for (let i = 0; i < 26; i++) {
          const ch = String.fromCharCode(65 + i);
          const pair = this.pairs.find((p) => p.includes(ch));
          out.push({ ch, pairedWith: pair ? pair.find((x) => x !== ch) || '' : '' });
        }
        return out;
      },
      chips() { return this.pairs.length ? this.pairs.map((p) => p.join('')).join(' ') : '未插线'; }
    },
    template: `
      <div>
        <div class="plugs-grid">
          <div v-for="s in sockets" :key="s.ch" class="plug"
               :class="{ paired: s.pairedWith, pending: pending === s.ch }"
               :title="s.pairedWith ? s.ch + ' ↔ ' + s.pairedWith : s.ch"
               @click="$emit('toggle', s.ch)">{{ s.ch }}</div>
        </div>
        <div class="plug-chips">{{ pending ? '再点一个字母与 ' + pending + ' 配对,再点 ' + pending + ' 取消' : chips }}</div>
      </div>`
  });

  /* 信号路径:12 节点单行,反射器红高亮,回程半透明 */
  app.component('signal-path', {
    props: { path: { type: Array, default: null } },
    computed: {
      reflIdx() { return this.path ? this.path.findIndex((q) => q.stage === '反射器') : -1; }
    },
    template: `
      <div class="path">
        <template v-if="path && path.length">
          <div v-for="(p, i) in path" :key="i" class="node"
               :class="{ refl: i === reflIdx, ret: i > reflIdx, fwd: i < path.length - 1 }">
            {{ p.stage }}<b>{{ p.letter }}</b>
          </div>
        </template>
        <span v-else class="hint">按键后显示电流全程</span>
      </div>`
  });
}
