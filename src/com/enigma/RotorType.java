package com.enigma;

/**
 * 恩尼格玛 I 型五个历史转子型号
 * wiring 为出厂固定接线(26 个字母的置换):下标为输入字母索引,字符为输出字母。
 * 例:转子 I 接线首字符为 'E',即 A 进 E 出;接线 1930 年定型的实物即如此,非算法生成
 * notch 为进位缺口位置:转子转到该字母时带动左侧转子步进
 *
 * @author echo
 * @date 2026-10-19
 */
public enum RotorType {

    /** 转子 I,1930 年列装,缺口 Q */
    ROTOR_I("I", "EKMFLGDQVZNTOWYHXUSPAIBRCJ", 'Q'),

    /** 转子 II,1930 年列装,缺口 E */
    ROTOR_II("II", "AJDKSIRUXBLHWTMCQGZNPYFVOE", 'E'),

    /** 转子 III,1930 年列装,缺口 V */
    ROTOR_III("III", "BDFHJLCPRTXVZNYEIWGAKMUSQO", 'V'),

    /** 转子 IV,1938 年 12 月列装,缺口 J */
    ROTOR_IV("IV", "ESOVPZJAYQUIRHXLNFTGKDCMWB", 'J'),

    /** 转子 V,1938 年 12 月列装,缺口 Z */
    ROTOR_V("V", "VZBRGITYUPSDNHLXAWMJQOFECK", 'Z');

    /** 型号名(罗马数字,如 "I") */
    private final String name;
    /** A→Z 接线表:下标为输入字母索引,字符为输出字母 */
    private final String wiring;
    /** 进位缺口字母:窗口转到该字母时带动左侧转子步进 */
    private final char notch;

    RotorType(String name, String wiring, char notch) {
        this.name = name;
        this.wiring = wiring;
        this.notch = notch;
    }

    public String displayName() {
        return name;
    }

    public String wiring() {
        return wiring;
    }

    public char notch() {
        return notch;
    }

    /**
     * 按罗马数字名称查找转子型号
     *
     * @param name 转子名称,如 "I" / "II"
     * @return 对应枚举,不存在返回 null
     */
    public static RotorType ofName(String name) {
        for (RotorType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
