/* ============================================================
 * 根应用:唯一状态源 + 服务端 API 交互 + 机器/手册/历史三视图
 * 设计约定:渲染全部状态驱动;发报与手动敲键共用 press() 单一出口
 * ============================================================ */

const enigmaApp = Vue.createApp({
  data() {
    return {
      /* 视图路由(单页三视图,不引路由库) */
      view: 'machine',
      tabs: [
        { key: 'machine', label: '机 器' },
        { key: 'manual', label: '操作手册' },
        { key: 'history', label: '背景与历史' }
      ],
      /* 配色 */
      theme: 'D',
      /* 服务端机器状态(以服务端为准) */
      state: { rotors: [], positions: 'AAA', plugs: '', reflector: 'B' },
      /* 左栏表单(装机时提交) */
      cfg: {
        rotorL: 'I', rotorM: 'II', rotorR: 'III',
        posL: 'A', posM: 'A', posR: 'A',
        ringL: 'A', ringM: 'A', ringR: 'A',
        refl: 'B'
      },
      /* 插线板 UI 状态 */
      plugPairs: [],
      pendingPlug: null,
      /* 电文与输出 */
      plain: '',
      cipher: '',
      /* 灯板/路径/状态栏 */
      lit: '',
      litTimer: null,
      path: null,
      status: '点击键盘或直接敲击物理键盘',
      /* 发报过程锁与中止标记 */
      busy: false,
      stopRequested: false,
      autoReset: true,
      /* 弹层 */
      toastMsg: '',
      toastTimer: null
    };
  },

  computed: {
    rotorNames() { return ['I', 'II', 'III', 'IV', 'V']; },
    letters26() { return Array.from({ length: 26 }, (_, i) => String.fromCharCode(65 + i)); },
    /* 转子鼓视图模型:由服务端状态推导 */
    drums() {
      if (!this.state.rotors || this.state.rotors.length < 3) { return []; }
      const labels = ['左', '中', '右'];
      return labels.map((label, i) => {
        const r = this.state.rotors[i];
        const code = this.state.positions.charCodeAt(i) - 65;
        return {
          label,
          name: r.name,
          ring: r.ring,
          notch: window.EnigmaShared.NOTCH[r.name] || '',
          letters: [(code + 25) % 26, code, (code + 1) % 26]
              .map((n) => String.fromCharCode(65 + n))
        };
      });
    },
    sendLabel() { return this.busy ? '停 止' : '整 段 发 报'; },
    /* 转子序三槽不可重复,重复时禁止装机并提示(提前于提交校验) */
    rotorOrderInvalid() {
      return this.cfg.rotorL === this.cfg.rotorM
          || this.cfg.rotorM === this.cfg.rotorR
          || this.cfg.rotorL === this.cfg.rotorR;
    }
  },

  watch: {
    /* 主题写回 body 与 localStorage */
    theme(v) {
      document.body.dataset.theme = v;
      localStorage.setItem('enigma-theme', v);
    },
    /* 切入视图时重渲染 EnigmaFlow 图:可见态下实测文字宽度,消除估算误差 */
    view(v) {
      this.$nextTick(() => {
        if (!window.EnigmaFlow) { return; }
        document.querySelectorAll('section[data-view="' + v + '"] .ef-container').forEach((box) => {
          if (box._efCfg) { window.EnigmaFlow.render(box, box._efCfg); }
        });
      });
    }
  },

  methods: {
    /* ---------- 基础设施 ---------- */
    toast(msg) {
      this.toastMsg = msg;
      clearTimeout(this.toastTimer);
      this.toastTimer = setTimeout(() => { this.toastMsg = ''; }, 3000);
    },
    async api(path) {
      const resp = await fetch(path);
      const body = await resp.json();
      if (!resp.ok) {
        throw new Error(body.errorMessage || body.userTip || ('HTTP ' + resp.status));
      }
      return body.data;
    },
    sleep(ms) { return new Promise((r) => setTimeout(r, ms)); },

    /* ---------- 主题 ---------- */
    applyTheme(t) {
      this.theme = t;
    },

    /* ---------- 状态同步 ---------- */
    /* 服务端状态 → 本地;表单回显,避免下拉框与机器脱节 */
    applyState(data) {
      this.state = data;
      this.cfg.rotorL = data.rotors[0].name;
      this.cfg.rotorM = data.rotors[1].name;
      this.cfg.rotorR = data.rotors[2].name;
      this.cfg.refl = data.reflector;
      /* 位置下拉框 = 装机目标,不回写实时位置(实时相位只体现在转子鼓) */
      this.cfg.ringL = data.rotors[0].ring;
      this.cfg.ringM = data.rotors[1].ring;
      this.cfg.ringR = data.rotors[2].ring;
      this.plugPairs = data.plugs
          ? data.plugs.split(/\s+/).filter(Boolean).map((p) => [p[0], p[1]])
          : [];
      this.pendingPlug = null;
    },
    async fetchState() {
      this.applyState(await this.api('/api/state'));
    },

    /* ---------- 装机:应用配置并复位现场 ---------- */
    async applyConfig() {
      const params = new URLSearchParams({
        rotors: [this.cfg.rotorL, this.cfg.rotorM, this.cfg.rotorR].join(','),
        positions: this.cfg.posL + this.cfg.posM + this.cfg.posR,
        rings: this.cfg.ringL + this.cfg.ringM + this.cfg.ringR,
        reflector: this.cfg.refl,
        plugs: this.plugPairs.map((p) => p.join('')).join(',')
      });
      try {
        const data = await this.api('/api/config?' + params.toString());
        this.applyState(data);
        /* 全部现场复位:密文、残亮灯、信号路径;明文纸带保留 */
        this.cipher = '';
        this.path = null;
        this.lit = '';
        this.status = '装机完成 · 位置 ' + this.state.positions + ' · 反射器 ' + this.state.reflector;
        return data;
      } catch (e) {
        this.toast('装机失败:' + e.message);
      }
    },

    /* ---------- 单键:手动与发报共用出口 ---------- */
    async press(letter, fromUser = true) {
      if (fromUser && this.busy) { return ''; }
      try {
        const data = await this.api('/api/keypress?letter=' + letter);
        this.state.positions = data.positions;
        this.path = data.path;
        this.lit = data.lamp;
        clearTimeout(this.litTimer);
        this.litTimer = setTimeout(() => { this.lit = ''; }, 450);
        /* 统一出口:两种入口都累积密文;手动敲击同时记入明文纸带 */
        this.cipher += data.lamp;
        if (fromUser) { this.plain += letter; }
        return data.lamp;
      } catch (e) {
        this.toast('机械故障:' + e.message);
        return '';
      }
    },

    /* ---------- 插线板 ---------- */
    togglePlug(ch) {
      const idx = this.plugPairs.findIndex((p) => p.includes(ch));
      if (idx >= 0) {
        this.plugPairs.splice(idx, 1);
      } else if (this.pendingPlug === null) {
        this.pendingPlug = ch;
      } else if (this.pendingPlug === ch) {
        this.pendingPlug = null;
      } else {
        if (this.plugPairs.length >= 13) { this.toast('插线最多 13 对'); this.pendingPlug = null; return; }
        this.plugPairs.push([this.pendingPlug, ch]);
        this.pendingPlug = null;
      }
    },

    /* ---------- 清空 ---------- */
    clearPlain() { this.plain = ''; },
    clearCipher() { this.cipher = ''; },

    /* ---------- 整段发报(可中止) ---------- */
    async sendTelegram() {
      const raw = this.plain.toUpperCase().replace(/[^A-Z]/g, '');
      if (!raw) { this.toast('明文不能为空(仅 A-Z)'); return; }
      this.busy = true;
      this.stopRequested = false;
      try {
        if (this.autoReset) { await this.applyConfig(); }
        for (let i = 0; i < raw.length; i++) {
          if (this.stopRequested) { this.status = '发报已停止(转子位置保持现状)'; return; }
          const lamp = await this.press(raw[i], false);
          if (!lamp) { break; }
          this.status = '发报中 ' + (i + 1) + '/' + raw.length + ' · 转子 ' + this.state.positions;
          await this.sleep(90);
        }
        this.status = '发报完成 · 密文已生成(复制给收方,同配置可解密)';
      } finally {
        this.busy = false;
        this.stopRequested = false;
      }
    },
    toggleSend() {
      if (this.busy) { this.stopRequested = true; } else { this.sendTelegram(); }
    },

    /* ---------- 示例装载(背景与历史视图联动) ---------- */
    async loadExample(name) {
      const set = (patch) => {
        Object.assign(this.cfg, patch.cfg || {});
        this.plugPairs = patch.plugs || [];
      };
      const P10 = [['A','D'],['C','N'],['E','T'],['F','L'],['G','I'],['J','V'],['K','Z'],['P','U'],['Q','Y'],['W','X']];
      const examples = {
        standard: { cfg: { rotorL:'I', rotorM:'II', rotorR:'III', ringL:'A', ringM:'A', ringR:'A', posL:'A', posM:'A', posR:'A', refl:'B' }, plugs: [] },
        ringBBB:  { cfg: { rotorL:'I', rotorM:'II', rotorR:'III', ringL:'B', ringM:'B', ringR:'B', posL:'A', posM:'A', posR:'A', refl:'B' }, plugs: [] },
        doubleStep:{ cfg: { rotorL:'I', rotorM:'II', rotorR:'III', ringL:'A', ringM:'A', ringR:'A', posL:'A', posM:'D', posR:'U', refl:'B' }, plugs: [] },
        aachen:   { cfg: { rotorL:'I', rotorM:'IV', rotorR:'III', ringL:'P', ringM:'Z', ringR:'H', posL:'Q', posM:'W', posR:'E', refl:'B' }, plugs: P10 }
      };
      const ex = examples[name];
      if (!ex) { return; }
      set(ex);
      await this.applyConfig();
      this.view = 'machine';
      this.toast('已装载示例配置:' + name + ',按文档操作即可复现');
    }
  },

  mounted() {
    /* 主题记忆(默认 D) */
    this.theme = localStorage.getItem('enigma-theme') || 'D';
    this.fetchState().catch((e) => this.toast('无法连接恩尼格玛服务:' + e.message));
  },

  template: `
  <div class="wrap">
    <header class="toolbar">
      <div class="toolbar-left">
        <div class="brand">
          <h1>ENIGMA&nbsp;I</h1>
          <p>恩尼格玛 I 型</p>
        </div>
        <nav class="tabs">
          <button v-for="t in tabs" :key="t.key" class="tab"
                  :class="{ active: view === t.key }" @click="view = t.key">{{ t.label }}</button>
        </nav>
      </div>
      <div class="theme-switch">
        <span class="ts-label">配色</span>
        <select v-model="theme" title="配色方案">
          <option value="A">胡桃木黄铜</option>
          <option value="B">仪表绿·战斗机灰</option>
          <option value="C">潜航红铜·深海蓝</option>
          <option value="D">布莱切利档案·纸墨</option>
        </select>
      </div>
    </header>

    <!-- 机器视图 -->
    <section v-show="view === 'machine'" data-view="machine" class="layout">
      <div class="panel">
        <h2>每日密钥配置</h2>
        <div class="cfg-row"><label>转子序</label>
          <div class="triple">
            <select v-model="cfg.rotorL"><option v-for="n in rotorNames" :key="n">{{ n }}</option></select>
            <select v-model="cfg.rotorM"><option v-for="n in rotorNames" :key="n">{{ n }}</option></select>
            <select v-model="cfg.rotorR"><option v-for="n in rotorNames" :key="n">{{ n }}</option></select>
          </div>
        </div>
        <div class="cfg-row"><label>初始位置</label>
          <div class="triple">
            <select v-model="cfg.posL"><option v-for="c in letters26" :key="c">{{ c }}</option></select>
            <select v-model="cfg.posM"><option v-for="c in letters26" :key="c">{{ c }}</option></select>
            <select v-model="cfg.posR"><option v-for="c in letters26" :key="c">{{ c }}</option></select>
          </div>
        </div>
        <div class="cfg-row"><label>环设置</label>
          <div class="triple">
            <select v-model="cfg.ringL"><option v-for="c in letters26" :key="c">{{ c }}</option></select>
            <select v-model="cfg.ringM"><option v-for="c in letters26" :key="c">{{ c }}</option></select>
            <select v-model="cfg.ringR"><option v-for="c in letters26" :key="c">{{ c }}</option></select>
          </div>
        </div>
        <div class="cfg-row"><label>反射器</label>
          <div class="triple"><select v-model="cfg.refl"><option>B</option><option>C</option></select></div>
        </div>
        <div class="hint">转子顺序 = 左|中|右。缺口:Ⅰ→Q Ⅱ→E Ⅲ→V Ⅳ→J Ⅴ→Z</div>
        <div class="hint" v-if="rotorOrderInvalid" style="color: var(--refl)">三个转子型号不能重复</div>
        <button class="btn" :disabled="rotorOrderInvalid" @click="applyConfig">装 机(重置位置)</button>

        <h2 style="margin-top:12px">插线板</h2>
        <plug-board :pairs="plugPairs" :pending="pendingPlug" @toggle="togglePlug"></plug-board>
        <div class="hint">点两个字母插一对,最多 13 对;再点已插线的字母拔线。</div>
      </div>

      <div class="panel machine-flex">
        <h2>转 子</h2>
        <div class="rotors">
          <rotor-drum v-for="d in drums" :key="d.label"
                      :label="d.label" :name="d.name" :ring="d.ring" :notch="d.notch" :letters="d.letters"></rotor-drum>
        </div>
        <h2>灯 板</h2>
        <lamp-board :lit="lit"></lamp-board>
        <h2>键 盘</h2>
        <key-board @press="press"></key-board>
        <signal-path :path="path"></signal-path>
        <div class="status">{{ status }}</div>
      </div>

      <div class="col">
        <div class="panel msg">
          <h2>电 文</h2>
          <label style="display:flex;justify-content:space-between;align-items:center">明文 · 报文纸带
            <button class="mini" @click="clearPlain">清 空</button></label>
          <label style="margin-top:0">敲键盘自动记录;发报时整段重新上机</label>
          <textarea v-model="plain" spellcheck="false" :disabled="busy"
                    placeholder="敲击键盘或在此粘贴报文字母(A-Z)"></textarea>
          <label style="display:flex;justify-content:space-between;align-items:center">密文输出
            <button class="mini" @click="clearCipher">清 空</button></label>
          <textarea :value="cipher" spellcheck="false" readonly></textarea>
          <label><input type="checkbox" v-model="autoReset" style="vertical-align:-2px"> 发报前先装机复位</label>
          <button class="btn" @click="toggleSend">{{ sendLabel }}</button>
          <div class="guide">敲机身键盘:实时上机加密,字母自动记入明文框。整段发报:默认先装机复位,再把明文框整段重新上机一遍。收发流程见【操作手册】。</div>
        </div>
      </div>
    </section>

    <!-- 手册视图 -->
    <section v-show="view === 'manual'" data-view="manual">
      <view-manual></view-manual>
    </section>

    <!-- 背景与历史视图 -->
    <section v-show="view === 'history'" data-view="history">
      <view-history></view-history>
    </section>
  </div>
  <div id="toast" v-show="toastMsg">{{ toastMsg }}</div>
  `
});

/* 装配:注册组件后挂载;物理键盘事件挂在 document 上,输入控件聚焦时忽略 */
registerComponents(enigmaApp);
window.registerManualView(enigmaApp);
window.registerHistoryView(enigmaApp);
const vm = enigmaApp.mount('#app');
window.vm = vm; /* 调试与回归测试钩子 */
document.addEventListener('keydown', (e) => {
  if (['TEXTAREA', 'INPUT', 'SELECT'].includes(e.target.tagName)) { return; }
  if (e.ctrlKey || e.metaKey || e.altKey) { return; }
  const ch = e.key.toUpperCase();
  if (ch >= 'A' && ch <= 'Z') { vm.press(ch); }
});
