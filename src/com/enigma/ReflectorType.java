package com.enigma;

/**
 * 恩尼格玛反射器型号(Umkehrwalze)
 * 26 线两两配对的自逆置换,无固定点(字母永不映射自身)
 *
 * @author echo
 * @date 2026-10-19
 */
public enum ReflectorType {

    /** 反射器 B,1930 年起主力配置 */
    REFLECTOR_B("B", "YRUHQSLDPXNGOKMIEBFZCWVJAT"),

    /** 反射器 C,1937 年启用 */
    REFLECTOR_C("C", "FVPJIAOYEDRZXWGCTKUQSBNMHL");

    /** 型号名(罗马数字/字母,如 "B") */
    private final String name;
    /** A→Z 接线表:下标为输入字母索引,字符为输出字母 */
    private final String wiring;

    ReflectorType(String name, String wiring) {
        this.name = name;
        this.wiring = wiring;
    }

    /**
     * 型号显示名
     *
     * @return 型号名,如 "B"
     */
    public String displayName() {
        return name;
    }

    /**
     * 原始接线表(26 字符)
     *
     * @return 接线字符串
     */
    public String wiring() {
        return wiring;
    }

    /**
     * 按名称查找反射器型号
     *
     * @param name 名称,如 "B"
     * @return 对应枚举,不存在返回 null
     */
    public static ReflectorType ofName(String name) {
        for (ReflectorType type : values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
