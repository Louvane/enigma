package com.enigma;

/**
 * 整机自检:历史测试向量 + 双步进异常 + 加解密互逆
 * 运行:java -cp out com.enigma.EnigmaSelfTest
 *
 * @author echo
 * @date 2026-10-19
 */
public class EnigmaSelfTest {

    /**
     * 依次执行全部检验;任一断言失败即抛 AssertionError 中止
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        testKnownVector();
        testDoubleStepping();
        testRoundTrip();
        testPlugboard();
        System.out.println("全部自检通过 ✓");
    }

    /**
     * 历史测试向量:Enigma I / 转子 I-II-III / 位置 AAA / 环 AAA /
     * 反射器 B / 无插线,"AAAAA" 应加密为 "BDZGO"
     */
    private static void testKnownVector() {
        EnigmaMachine machine = buildDefault();
        StringBuilder cipher = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            cipher.append(machine.pressKey('A').lamp());
        }
        assertEquals("BDZGO", cipher.toString(), "历史向量 AAAAA→BDZGO");
    }

    /**
     * 双步进异常:右转子 III(缺口 V)、中转子 II(缺口 E)。
     * 中转子到 E 后,即使右转子不在 V,下一键中转子也要步进并带动左转子
     */
    private static void testDoubleStepping() {
        // 左 I 位置 A,中 II 位置 D,右 III 位置 U(均非缺口)
        EnigmaMachine machine = new EnigmaMachine(
                new Rotor(RotorType.ROTOR_I, 0, 0),
                new Rotor(RotorType.ROTOR_II, 'D' - 'A', 0),
                new Rotor(RotorType.ROTOR_III, 'U' - 'A', 0),
                new Reflector(ReflectorType.REFLECTOR_B),
                new Plugboard(new int[][] {}));
        assertEquals("ADU", machine.rotorPositions(), "初始位置");
        machine.pressKey('A');
        assertEquals("ADV", machine.rotorPositions(), "U→V,常规步进");
        machine.pressKey('A');
        // V 为 III 缺口:本键后中转子进位 D→E
        assertEquals("AEW", machine.rotorPositions(), "V→W 且中转子进位 D→E");
        machine.pressKey('A');
        // E 为 II 缺口:双步进!中转子再走 E→F,左转子被带动 A→B
        assertEquals("BFX", machine.rotorPositions(), "双步进:中 E→F 且左转子进位");
    }

    /**
     * 互逆性:同一配置下加密再"加密"应还原明文
     */
    private static void testRoundTrip() {
        String plain = "ENIGMAMACHINEREVEALED";
        EnigmaMachine encryptor = buildDefault();
        StringBuilder cipher = new StringBuilder(plain.length());
        for (char c : plain.toCharArray()) {
            cipher.append(encryptor.pressKey(c).lamp());
        }
        EnigmaMachine decryptor = buildDefault();
        StringBuilder recovered = new StringBuilder(plain.length());
        for (char c : cipher.toString().toCharArray()) {
            recovered.append(decryptor.pressKey(c).lamp());
        }
        assertEquals(plain, recovered.toString(), "同配置加解密互逆");
    }

    /**
     * 插线板:AB 互换应改变结果,且插线板自身可逆
     */
    private static void testPlugboard() {
        EnigmaMachine plugged = new EnigmaMachine(
                new Rotor(RotorType.ROTOR_I, 0, 0),
                new Rotor(RotorType.ROTOR_II, 0, 0),
                new Rotor(RotorType.ROTOR_III, 0, 0),
                new Reflector(ReflectorType.REFLECTOR_B),
                new Plugboard(new int[][] {{0, 1}}));
        char lamp = plugged.pressKey('A').lamp();
        if (lamp == 'A') {
            throw new AssertionError("反射器无固定点,密文不应等于明文");
        }
    }

    /**
     * 构造标准检验配置:转子 I-II-III / 位置 AAA / 环 AAA / 反射器 B / 无插线
     *
     * @return 全新整机
     */
    private static EnigmaMachine buildDefault() {
        return new EnigmaMachine(
                new Rotor(RotorType.ROTOR_I, 0, 0),
                new Rotor(RotorType.ROTOR_II, 0, 0),
                new Rotor(RotorType.ROTOR_III, 0, 0),
                new Reflector(ReflectorType.REFLECTOR_B),
                new Plugboard(new int[][] {}));
    }

    /**
     * 断言相等,失败抛 AssertionError 并附标签与两侧值
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param label    用例标签(输出用)
     */
    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " 失败:期望 " + expected + ",实际 " + actual);
        }
        System.out.println("通过:" + label);
    }
}
