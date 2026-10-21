/* ============================================================
 * EnigmaFlow —— 轻量声明式 SVG 流程图引擎(参考"教程站"骨架)
 * 特性:
 *   · render(container, cfg)   声明式渲染:节点圆角卡 + 细线箭头 + 分组框
 *   · animateStations(...)     电流逐站点亮动画(恩尼格玛专属语义)
 *   · 主题零耦合:全部颜色走 CSS 变量,四主题自动跟随
 *   · 防叠加:每容器独立定时器,重渲染前清理;回调检查 isConnected
 * ============================================================ */
window.EnigmaFlow = (function () {
  const NS = 'http://www.w3.org/2000/svg';
  const MIN_W = 44, MAX_W = 220, NODE_H = 34, PAD_X = 20;
  /* Canvas 精确测宽:无需 DOM 渲染,CJK/混排/全角符号全部准确 */
  const measureCtx = document.createElement('canvas').getContext('2d');

  const timers = new Map();   // container → interval id

  /* ---------- 工具 ---------- */
  function el(name, attrs) {
    const e = document.createElementNS(NS, name);
    for (const k in (attrs || {})) { e.setAttribute(k, attrs[k]); }
    return e;
  }
  function textWidth(label) {
    measureCtx.font = '500 13px "SF Mono", Menlo, "Songti SC", monospace';
    return measureCtx.measureText(label).width;
  }
  function nodeSize(label, overrideW) {
    const w = overrideW
        ? overrideW
        : Math.max(MIN_W, Math.min(MAX_W, Math.ceil(textWidth(label)) + PAD_X));
    return { w, h: NODE_H };
  }
  function stop(container) {
    const id = timers.get(container);
    if (id) { clearInterval(id); timers.delete(container); }
  }

  /* ---------- 渲染 ----------
   * cfg = {
   *   width, height,
   *   nodes: [{ id, label, cx, cy, variant? }]        variant: refl|key|lamp
   *   edges: [{ from, to }]                            直线,箭头指向 to
   *   groups: [{ label, x, y, w, h }]                  分组框(虚线)+ 组标题
   * }
   * 节点用中心坐标 cx/cy,宽高由文字自动估算。
   */
  function render(container, cfg) {
    if (!container || !container.isConnected) { return; }
    stop(container);
    container.replaceChildren();

    const svg = el('svg', {
      viewBox: '0 0 ' + cfg.width + ' ' + cfg.height,
      class: 'ef-svg', preserveAspectRatio: 'xMidYMid meet'
    });
    container.classList.add('ef-ready');
    container.appendChild(svg);   /* 先挂载:文字度量需真实 DOM 环境 */

    /* 箭头 marker:currentColor,主题自动跟随 */
    const defs = el('defs', {});
    const markerId = 'ef-arrow-' + Math.random().toString(36).slice(2, 8);
    const marker = el('marker', {
      id: markerId, viewBox: '0 0 10 10', refX: 9, refY: 5,
      markerWidth: 7, markerHeight: 7, orient: 'auto'
    });
    marker.appendChild(el('path', { d: 'M 0 0 L 10 5 L 0 10 z', class: 'ef-arrow-head' }));
    defs.appendChild(marker);
    svg.appendChild(defs);

    /* 计算节点几何:autoSpread=true 时按宽度自动等距铺满画布 */
    let nodesCfg = cfg.nodes;
    if (cfg.autoSpread) {
      const padX = cfg.padX || 20;
      const widths = cfg.nodes.map((n) => nodeSize(n.label, n.w).w);
      const total = widths.reduce((a, b) => a + b, 0);
      /* 相邻矩形间隙相等;画布宽度恒定 → 全站流程图缩放一致、元素等大 */
      const gap = (cfg.width - padX * 2 - total) / (cfg.nodes.length - 1);
      let cx = padX + widths[0] / 2;
      nodesCfg = cfg.nodes.map((n, i) => {
        const c = Object.assign({}, n, { cx: Math.round(cx) });
        const nextHalf = (widths[i + 1] || 0) / 2;
        cx += widths[i] / 2 + gap + nextHalf;
        return c;
      });
    }
    const geo = {};
    nodesCfg.forEach((n) => {
      const s = nodeSize(n.label, n.w);
      geo[n.id] = { x: n.cx - s.w / 2, y: n.cy - s.h / 2, w: s.w, h: s.h, cx: n.cx, cy: n.cy, label: n.label, variant: n.variant || '' };
    });

    /* 1. 分组框(在最后面) */
    (cfg.groups || []).forEach((g) => {
      svg.appendChild(el('rect', {
        x: g.x, y: g.y, width: g.w, height: g.h, rx: 10, class: 'ef-cluster'
      }));
      const t = el('text', { x: g.x + g.w / 2, y: g.y - 8, class: 'ef-group-label', 'text-anchor': 'middle' });
      t.textContent = g.label;
      svg.appendChild(t);
    });

    /* 2. 节点(底层,先画) */
    cfg.nodes.forEach((n) => {
      const g = geo[n.id];
      const group = el('g', { class: 'ef-node' + (n.variant ? ' ef-' + n.variant : ''), 'data-id': n.id });
      group.appendChild(el('rect', { x: g.x, y: g.y, width: g.w, height: g.h, rx: 9 }));
      const t = el('text', { x: g.cx, y: g.cy + 1, 'text-anchor': 'middle', 'dominant-baseline': 'middle' });
      t.textContent = n.label;
      group.appendChild(t);
      svg.appendChild(group);
    });

    /* 3. 实测修正:容器可见时用真实文字宽度对称加宽节点(隐藏态跳过,保持估算) */
    if (container.offsetParent !== null) {
      cfg.nodes.forEach((n) => {
        const g = geo[n.id];
        const group = svg.querySelector('.ef-node[data-id="' + n.id + '"]');
        if (!group) { return; }
        const t = group.querySelector('text');
        const tl = t.getComputedTextLength();
        if (!tl) { return; }
        const need = Math.ceil(tl) + PAD_X + 14;
        if (need > g.w) {
          const d = (need - g.w) / 2;
          const rect = group.querySelector('rect');
          rect.setAttribute('x', Math.round(g.x - d));
          rect.setAttribute('width', need);
          g.w = need;
          g.x -= d;
        }
      });
    }

    /* 4. 边(最上层):直线 + 可选虚线 + 可选中点标签 */
    (cfg.edges || []).forEach((e) => {
      const a = geo[e.from], b = geo[e.to];
      if (!a || !b) { return; }
      const x1 = a.cx + a.w / 2 + 2, y1 = a.cy;
      const x2 = b.cx - b.w / 2 - 5, y2 = b.cy;
      const p = el('path', {
        d: 'M ' + x1 + ' ' + y1 + ' L ' + x2 + ' ' + y2,
        class: 'ef-edge', 'marker-end': 'url(#' + markerId + ')'
      });
      if (e.dash) { p.setAttribute('stroke-dasharray', '5 4'); }
      svg.appendChild(p);
      if (e.label) {
        const t = el('text', {
          x: (x1 + x2) / 2, y: (y1 + y2) / 2 + (e.below ? 30 : -8),
          class: 'ef-edge-label', 'text-anchor': 'middle'
        });
        t.textContent = e.label;
        svg.appendChild(t);
      }
    });

    container._efCfg = cfg;   // 缓存配置供重播
  }

  /* ---------- 电流动画:按站位序列逐站点亮 ----------
   * container: 已 render 的容器
   * order:     站位 id 序列(与 keypress 路径顺序一致)
   * opts:      { interval: ms, hold: 终态保持(默认 true) }
   * 逐站加 .lit;反射器站额外加 .hit(脉冲)。返回 Promise,动画完成 resolve。
   */
  function animateStations(container, order, opts) {
    const o = opts || {};
    const interval = o.interval || 140;
    stop(container);
    return new Promise((resolve) => {
      const svg = container.querySelector('svg');
      if (!svg || !svg.isConnected) { resolve(); return; }
      const nodeGroups = {};
      container.querySelectorAll('.ef-node').forEach((g) => {
        nodeGroups[g.dataset.id] = g;
        g.classList.remove('lit', 'hit');
      });
      let i = 0;
      const timer = setInterval(() => {
        if (!svg.isConnected) { clearInterval(timer); timers.delete(container); resolve(); return; }
        if (i >= order.length) {
          clearInterval(timer); timers.delete(container);
          resolve();
          return;
        }
        const g = nodeGroups[order[i]];
        if (g) {
          g.classList.add('lit');
          if (g.classList.contains('ef-refl')) { g.classList.add('hit'); }
        }
        i++;
      }, interval);
      timers.set(container, timer);
    });
  }

  /* 重播:清灯 → 重新按缓存序列点亮 */
  function replay(container, order, opts) {
    return animateStations(container, order, Object.assign({ interval: 140 }, opts));
  }

  /* ---------- 恩尼格玛信号路径标准图(历史视图用) ---------- */
  const SIGNAL_CFG = {
    width: 1000, height: 138,
    nodes: [
      { id: 'K', label: '按键', cx: 48, cy: 74, w: 56, variant: 'key' },
      { id: 'PB1', label: '插线板', cx: 138, cy: 74, w: 72 },
      { id: 'ETW', label: 'ETW', cx: 234, cy: 74, w: 68 },
      { id: 'R3', label: '右', cx: 318, cy: 74, w: 48 },
      { id: 'R2', label: '中', cx: 392, cy: 74, w: 48 },
      { id: 'R1', label: '左', cx: 466, cy: 74, w: 48 },
      { id: 'REF', label: '反射器', cx: 551, cy: 74, w: 70, variant: 'refl' },
      { id: 'R1B', label: '左⁻¹', cx: 636, cy: 74, w: 48 },
      { id: 'R2B', label: '中⁻¹', cx: 710, cy: 74, w: 48 },
      { id: 'R3B', label: '右⁻¹', cx: 784, cy: 74, w: 48 },
      { id: 'PB2', label: '插线板', cx: 870, cy: 74, w: 72 },
      { id: 'LAMP', label: '灯板', cx: 960, cy: 74, w: 56, variant: 'lamp' }
    ],
    edges: [
      { from: 'K', to: 'PB1' }, { from: 'PB1', to: 'ETW' },
      { from: 'ETW', to: 'R3' }, { from: 'R3', to: 'R2' }, { from: 'R2', to: 'R1' },
      { from: 'R1', to: 'REF' },
      { from: 'REF', to: 'R1B' }, { from: 'R1B', to: 'R2B' }, { from: 'R2B', to: 'R3B' },
      { from: 'R3B', to: 'PB2' }, { from: 'PB2', to: 'LAMP' }
    ],
    groups: [
      { label: '三转子 · 去程', x: 288, y: 42, w: 208, h: 56 },
      { label: '三转子 · 回程', x: 606, y: 42, w: 208, h: 56 }
    ]
  };
  /* keypress 路径 stage 名 → 图节点 id(顺序即电流顺序) */
  const SIGNAL_ORDER = ['K', 'PB1', 'ETW', 'R3', 'R2', 'R1', 'REF', 'R1B', 'R2B', 'R3B', 'PB2', 'LAMP'];

  return { render, animateStations, replay, stop, SIGNAL_CFG, SIGNAL_ORDER };
})();
