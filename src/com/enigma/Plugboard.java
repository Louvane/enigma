package com.enigma;

/**
 * 恩尼格玛插线板(Steckerbrett)
 * 对应物理部件:面板上 26 个插孔,操作员用双头线把字母两两交换,
 * 早期 6 根、战争后期增至 10 根,极限 13 对;未插线的字母直通。
 * 信号出、入各经过一次插线板,因自逆,两次作用刚好抵消。
 *
 * @author echo
 * @date 2026-10-19
 */
public class Plugboard {

    /** 历史标准配置的最大插线对数 */
    public static final int MAX_PLUG_PAIRS = 13;

    /** 交换映射:下标为输入字母索引,值为配对字母索引;未插线处等于自身 */
    private final int[] swapMap = new int[Rotor.ALPHABET_SIZE];

    /**
     * 按插线对构造插线板
     *
     * @param pairs 每对两个 0-25 索引,重复字母或自环将抛出 IllegalArgumentException
     */
    public Plugboard(int[][] pairs) {
        for (int i = 0; i < Rotor.ALPHABET_SIZE; i++) {
            swapMap[i] = i;
        }
        for (int[] pair : pairs) {
            int a = pair[0];
            int b = pair[1];
            if (a == b || swapMap[a] != a || swapMap[b] != b) {
                throw new IllegalArgumentException("非法插线对:" + (char) ('A' + a) + (char) ('A' + b));
            }
            swapMap[a] = b;
            swapMap[b] = a;
        }
    }

    /**
     * 信号经过插线板(进出方向共用,插线板自逆)
     *
     * @param pin 输入 0-25
     * @return 交换后的 0-25
     */
    public int swap(int pin) {
        return swapMap[pin];
    }
}
