/* ============================================================
 * 视图:操作手册
 * 定位:只回答"怎么用"——上手、规程、界面解读、输入惯例、
 *       控件参考、操作类常见问题。原理与历史一律指向【背景与历史】。
 * ============================================================ */
function registerManualView(app) {
  app.component('view-manual', {
    mounted() {
      if (!window.EnigmaFlow) { return; }
      /* 发方流程 */
      const send = document.getElementById('ef-sendrecv');
      if (send) {
        window.EnigmaFlow.render(send, {
          width: 1000, height: 130, autoSpread: true,
          nodes: [
            { id: 'S1', label: '① 编密钥 RTZ', cy: 60 },
            { id: 'S2', label: '② 拨 QWE · 装机', cy: 60 },
            { id: 'S3', label: '③ 敲 RTZ · 读 EWG', cy: 60 },
            { id: 'S4', label: '④ 拨 RTZ · 装机', cy: 60 },
            { id: 'S5', label: '⑤ 逐键敲正文', cy: 60 },
            { id: 'S6', label: '⑥ 发报头与密文', cy: 60 }
          ],
          edges: [
            { from: 'S1', to: 'S2' }, { from: 'S2', to: 'S3' }, { from: 'S3', to: 'S4' },
            { from: 'S4', to: 'S5' }, { from: 'S5', to: 'S6' }
          ]
        });
      }
      /* 收方流程 */
      const recv = document.getElementById('ef-recv');
      if (recv) {
        window.EnigmaFlow.render(recv, {
          width: 1000, height: 130, autoSpread: true,
          nodes: [
            { id: 'R1', label: '① 照日钥装机', cy: 60 },
            { id: 'R2', label: '② 拨 QWE · 敲 EWG · 读 RTZ', cy: 60 },
            { id: 'R3', label: '③ 位置拨 RTZ · 装机', cy: 60 },
            { id: 'R4', label: '④ 敲密文 → 出明文', cy: 60 }
          ],
          edges: [
            { from: 'R1', to: 'R2' }, { from: 'R2', to: 'R3' }, { from: 'R3', to: 'R4' }
          ]
        });
      }
    },
    template: `
    <div class="doc">
      <h3>三分钟上手</h3>
      <p>最小可用循环:左栏配置密钥 → 点【装 机】→ 敲键盘,灯板亮出的字母就是密文,机身下沿的信号路径同步显示电流全程。</p>
      <ol>
        <li>左栏选转子序、环设置、反射器(初学保持 <code>I,II,III / AAA / B</code>);</li>
        <li>点【装 机】——转子复位,密文与路径清空;</li>
        <li>点屏幕键盘或直接敲物理键盘:每敲一键,转子步进一格,灯板亮出密文字母;</li>
        <li>想发整句:在明文框打好字(或粘贴),点【整 段 发 报】,机器自动逐键敲完;发报过程中按钮变为【停 止】,可随时中止。</li>
      </ol>
      <div class="callout">同一配置下,把密文再"加密"一遍就还原明文——反射器自逆,加密与解密是同一个动作,没有模式开关。</div>

      <h3>发报与接收流程图</h3>
      <div class="ef-container" id="ef-sendrecv"></div>
      <div class="ef-container" id="ef-recv" style="margin-top:8px"></div>
      <p style="font-size:12px;color:var(--muted);margin-top:6px">报文头 QWE EWG 与正文密文经无线电传递;两行步骤一一对应,收方照做即可还原。</p>      <p style="font-size:12px;color:var(--muted)">报文头 QWE EWG 与正文密文经无线电传递给收方;两列步骤一一对应。</p>

      <h3>读懂界面</h3>
      <table>
        <tr><th>部件</th><th>怎么读</th></tr>
        <tr><td>转子鼓</td><td>窗口显示三格字母,中间高亮格是当前位置;鼓下方标注该轮的环设置与缺口字母,缺口对准当前位置时出现"◀ 缺口"标记——下一步该轮将进位。</td></tr>
        <tr><td>灯板</td><td>每次敲键亮一盏,约半秒后熄灭;亮起的字母即该次加密输出。</td></tr>
        <tr><td>信号路径</td><td>上一次按键的电流全程,共 12 站:红色为反射器(电流在此折返),半透明为回程各站。两行对齐着读——回程的"左转子⁻¹"正对去程的"左转子"。</td></tr>
        <tr><td>明文 · 报文纸带</td><td>你敲过的每一个键都会自动记录在这里,相当于操作员面前的电报纸。</td></tr>
        <tr><td>密文输出</td><td>所有按键(手动与整段发报)的灯板结果持续累积;收报时它显示的则是解出的明文。</td></tr>
      </table>

      <h3>清晨:装日钥(一天一次)</h3>
      <p>密钥单上印着当天全网的配置:转子序、环设置、插线板。照抄进左栏,点【装 机】。这三样全天不动——它们属于"日钥",与报文无关。</p>

      <h3>发送一份报文(每份重复此循环)</h3>
      <table>
        <tr><th>步</th><th>动作</th><th>页面操作</th><th>示例</th></tr>
        <tr><td>1</td><td>随手编 3 字母报文密钥</td><td>不用操作,记住即可;避免 <code>QWE</code>、<code>AAA</code> 这类规律串</td><td><code>RTZ</code></td></tr>
        <tr><td>2</td><td>转子拨到基本位</td><td>位置下拉选 <code>QWE</code> → 【装 机】</td><td>转子 QWE</td></tr>
        <tr><td>3</td><td>加密报文密钥</td><td>敲 <code>R T Z</code></td><td>灯亮 <code>EWG</code></td></tr>
        <tr><td>4</td><td>转子拨到报文密钥</td><td>位置下拉选 <code>RTZ</code> → 【装 机】</td><td>转子 RTZ</td></tr>
        <tr><td>5</td><td>加密正文</td><td>逐键敲入正文</td><td>密文输出</td></tr>
        <tr><td>6</td><td>发报</td><td>抄下 <code>QWE EWG</code> + 密文,按 5 字母一组拍发</td><td>—</td></tr>
      </table>

      <p>报文头 <code>QWE EWG</code> 里:QWE 是明写的基本位,EWG 是加密后的报文密钥。收方凭它还原 RTZ——整个过程详见下一步。</p>

      <h3>接收与解密</h3>
      <ol>
        <li>照同一张日钥装机(与发方清晨完全一致);</li>
        <li>读报文头 <code>QWE EWG</code>:位置拨 <code>QWE</code> →【装 机】→ 敲 <code>E W G</code>,灯板读出 <code>RTZ</code>;</li>
        <li>位置拨 <code>RTZ</code> →【装 机】;</li>
        <li>把收到的正文密文粘进明文框,逐键敲入(或直接【整 段 发 报】),密文框输出的就是明文。</li>
      </ol>
      <div class="callout">解密时"密文框显示的是明文"——因为输出框永远是灯板结果,敲什么由你决定。</div>

      <h3>历史输入惯例(报文该怎么写)</h3>
      <table>
        <tr><th>惯例</th><th>例</th><th>原因</th></tr>
        <tr><td>数字拼成德文单词</td><td>1→<code>EINS</code> 5→<code>FUENF</code> 0→<code>NULL</code></td><td>机器只有 26 个字母键</td></tr>
        <tr><td>空格/句号 → <code>X</code></td><td><code>AACHEN IST.</code> → <code>AACHENXISTX</code></td><td>无标点键</td></tr>
        <tr><td>叠字插 <code>Q</code></td><td><code>BALLE</code> → <code>BALQLE</code></td><td>重复模式是破译线索</td></tr>
        <tr><td><code>CH</code>/<code>CK</code> → <code>Q</code></td><td><code>DURCH</code> → <code>DURQ</code></td><td>Q 在德文里极罕见</td></tr>
        <tr><td>密文按 5 字母分组</td><td><code>EJZLB SYEQP</code></td><td>传输格式,与加密无关</td></tr>
      </table>

      <h3>控件参考</h3>
      <table>
        <tr><th>控件</th><th>作用</th><th>影响范围</th></tr>
        <tr><td>【装 机】</td><td>应用左栏全部配置</td><td>转子复位、密文清空、路径重置、灯灭;明文纸带保留</td></tr>
        <tr><td>明文【清 空】</td><td>清空报文纸带</td><td>只清明文框</td></tr>
        <tr><td>密文【清 空】</td><td>清空输出</td><td>只清密文框</td></tr>
        <tr><td>【整 段 发 报】/【停 止】</td><td>自动逐键敲入明文框内容 / 中止发报</td><td>与手动敲键共用同一台机器</td></tr>
        <tr><td>插线板圆钮</td><td>点两孔插线,点已配对孔拔线</td><td>点【装 机】后生效到机器</td></tr>
        <tr><td>配色下拉(右上)</td><td>四种主题换肤</td><td>仅外观,选择被记忆</td></tr>
      </table>

      <h3>操作类常见问题</h3>
      <ul>
        <li><b>发完一份忘了拨位置?</b>转子接着上一份的停位走,收方必然解错——每份报文都要重新选位置并【装 机】。</li>
        <li><b>改了插线没生效?</b>插线选择只是"摆在面板上",点【装 机】才写入机器。</li>
        <li><b>页面显示旧数据?</b>响应已禁缓存;若仍异常,强制刷新一次(Cmd+Shift+R)。</li>
        <li><b>物理键盘没反应?</b>焦点在输入框/下拉框时会被忽略,这是防误触设计。</li>
        <li><b>为什么不能直接输数字?</b>机器只有 26 个字母键——按上方"历史输入惯例"拼写即可,当年的操作员也是这么做的。</li>
      </ul>

      <h3>检验向量:照着做,验证机器没问题</h3>
      <p>以下配置与期望输出全部出自公开文献,点"装载"即可一键配置好机器,再按"操作"列亲手复现:</p>
      <table>
        <tr><th>向量</th><th>配置</th><th>操作 → 期望</th><th></th></tr>
        <tr><td>标准检验</td><td>I II III / 环AAA / 反射器B / 无插线</td><td>位置AAA,敲 5 个 <code>A</code> → <code>BDZGO</code></td>
            <td><button class="mini" @click="$root.loadExample('standard')">装 载</button></td></tr>
        <tr><td>环检验</td><td>同上,环改为 <code>BBB</code></td><td>敲 5 个 <code>A</code> → <code>EWTYX</code></td>
            <td><button class="mini" @click="$root.loadExample('ringBBB')">装 载</button></td></tr>
        <tr><td>双步进</td><td>I II III / 环AAA / 位置 <code>ADU</code></td><td>敲 3 个 <code>A</code>,窗口 <code>ADV→AEW→BFX</code></td>
            <td><button class="mini" @click="$root.loadExample('doubleStep')">装 载</button></td></tr>
        <tr><td>Aachen 战报</td><td>I IV III / 环 <code>PZH</code> / 插线 10 对</td><td>位置 <code>QWE</code> 敲 <code>RTZ</code> → <code>EWG</code>;拨 <code>RTZ</code> 解 <code>EJZLB…</code> → <code>XAACHEN…</code></td>
            <td><button class="mini" @click="$root.loadExample('aachen')">装 载</button></td></tr>
      </table>

      <h3>进阶自测:三问三答</h3>
      <p>操作贯通之后,这三问标志着从"会用"进入"懂原理"。先自己想,再展开对答案:</p>
      <ol>
        <li><b>为什么中转子会连续两键步进?</b><br>
            机械咬合的副产品:中转子的缺口拨杆与左转子咬合成一体。中转子被右转子带到缺口后,下一次按键拨杆弹回,同时推中、左两盘——于是中转子连跳两格。不是设计意图,模拟器必须还原它,否则密文全错(用上方"双步进"向量可验证)。</li>
        <li><b>密文里为什么永远找不到"字母对应位置出现同一个字母"?</b><br>
            反射器把 26 个字母两两配对、没有固定点,电流必然沿另一根线返回——按 <code>A</code> 亮 <code>A</code> 在物理上不可能。这个"方便的代价"正是图灵 crib 攻击的排除法立足点。</li>
        <li><b>1.59×10²⁰ 的密钥空间为什么没能保住它?</b><br>
            破译从未正面攻打密钥空间:双发报文密钥、可预测的天气报文、操作员的懒密钥、缴获的密钥单——规程漏洞一层层把侧门打开,Bombe 只是把这些漏洞机电化利用。机器无解,规程送命。</li>
      </ol>
    </div>`
  });
}
