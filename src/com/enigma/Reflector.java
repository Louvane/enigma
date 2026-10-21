package com.enigma;

/**
 * 恩尼格玛反射器
 * 对应物理部件:旋在机器左端的固定接线盘,不随转子步进、无环设置,
 * 信号到达后按两两配对折返。
 * 自逆性带来两大后果:①同一设置下加密与解密是同一操作,收发双方
 * 无需切换模式;②字母永不映射自身(无固定点)——恰恰是图灵 crib
 * 攻击的立足点,成也萧何败也萧何。
 *
 * @author echo
 * @date 2026-10-19
 */
public class Reflector {

    /** 反射器型号(B/C),决定 26 线配对方式 */
    private final ReflectorType type;
    /** 配对映射:下标为输入引脚,值为折返输出引脚(两两互换、无固定点) */
    private final int[] wiringMap = new int[Rotor.ALPHABET_SIZE];

    /**
     * 按型号构建反射器,接线表在构造时固化
     *
     * @param type 反射器型号
     */
    public Reflector(ReflectorType type) {
        this.type = type;
        String wiring = type.wiring();
        for (int in = 0; in < Rotor.ALPHABET_SIZE; in++) {
            wiringMap[in] = wiring.charAt(in) - 'A';
        }
    }

    /**
     * 信号进入反射器并折返
     *
     * @param pin 输入引脚 0-25
     * @return 折返输出引脚 0-25
     */
    public int reflect(int pin) {
        return wiringMap[pin];
    }

    /**
     * 反射器型号(B 或 C)
     *
     * @return 型号枚举
     */
    public ReflectorType getType() {
        return type;
    }
}
