package com.enigma;

/**
 * 恩尼格玛转子——机器的灵魂部件
 * 对应物理部件:黄铜圆盘,盘内 26 根出厂烧死的交叉接线(置换 W),
 * 盘缘字母环随盘转动(角位置 pos),接线板相对字母环可整体旋拧错位
 * (环设置 ring),字母环上另有进位缺口(notch)。
 *
 * 信号变换的数学本质是共轭置换:S = P^-1 · W · P。
 * 直觉:先把引脚号"转进"转子自身坐标系(position - ringSetting),
 * 查静态接线表,再把结果"转回"机器坐标系。环设置因此等效于给
 * 同一套接线叠加一个不随步进改变的整体偏移。
 *
 * @author echo
 * @date 2026-10-19
 */
public class Rotor {

    /** 字母表长度 */
    public static final int ALPHABET_SIZE = 26;

    /** 转子型号(接线/缺口均来自型号定义) */
    private final RotorType type;
    /** 正向接线表:下标为输入引脚,值为输出引脚(已把接线表转成数字) */
    private final int[] forwardMap = new int[ALPHABET_SIZE];
    /** 反向接线表:正向表的逆映射,回程穿转子时使用 */
    private final int[] backwardMap = new int[ALPHABET_SIZE];
    /** 当前角位置 0-25(0 对应窗口字母 A) */
    private int position;
    /** 环设置 0-25(接线相对字母环的整体偏移) */
    private int ringSetting;

    /**
     * 构建转子:接线在构造时固化,位置与环可在运行中步进
     *
     * @param type        转子型号
     * @param position    初始角位置(0-25,对应窗口字母 A-Z)
     * @param ringSetting 环设置(0-25,接线相对字母环的偏移)
     */
    public Rotor(RotorType type, int position, int ringSetting) {
        this.type = type;
        this.position = position;
        this.ringSetting = ringSetting;
        // 预计算正向接线表及其逆表(反向路径查逆表)
        String wiring = type.wiring();
        for (int in = 0; in < ALPHABET_SIZE; in++) {
            int out = wiring.charAt(in) - 'A';
            forwardMap[in] = out;
            backwardMap[out] = in;
        }
    }

    /**
     * 信号正向穿过转子(电流从右向左)
     * 三步:①转进转子坐标系 ②查固定接线 ③转回机器坐标系
     *
     * @param pin 输入引脚 0-25
     * @return 输出引脚 0-25
     */
    public int forwardPass(int pin) {
        int shifted = Math.floorMod(pin + position - ringSetting, ALPHABET_SIZE);
        int wired = forwardMap[shifted];
        return Math.floorMod(wired - position + ringSetting, ALPHABET_SIZE);
    }

    /**
     * 信号反向穿过转子(反射器折返后从左向右)
     * 与正向共用同一坐标变换,只是查接线逆表:正向 E→K,反向即 K→E
     *
     * @param pin 输入引脚 0-25
     * @return 输出引脚 0-25
     */
    public int backwardPass(int pin) {
        int shifted = Math.floorMod(pin + position - ringSetting, ALPHABET_SIZE);
        int wired = backwardMap[shifted];
        return Math.floorMod(wired - position + ringSetting, ALPHABET_SIZE);
    }

    /**
     * 是否停在进位缺口位置。
     * 缺口固定在字母环上而非接线板上,故只看 position、与 ringSetting
     * 无关——这是真实机械的关键细节,搞反会导致进位时机错乱
     *
     * @return 缺口对准时 true
     */
    public boolean atNotch() {
        return position == type.notch() - 'A';
    }

    /**
     * 步进一格。真实机器中步进由按键的机械联动在通电之前完成,
     * 因此第一个字母是在"初始位置 +1"处被加密的
     */
    public void step() {
        position = (position + 1) % ALPHABET_SIZE;
    }

    /**
     * 当前窗口字母(位置对应的 A-Z 显示)
     *
     * @return 窗口字母
     */
    public char positionLetter() {
        return (char) ('A' + position);
    }

    /**
     * 当前角位置索引(0-25)
     *
     * @return 位置索引
     */
    public int getPosition() {
        return position;
    }

    public RotorType getType() {
        return type;
    }

    public int getRingSetting() {
        return ringSetting;
    }
}
