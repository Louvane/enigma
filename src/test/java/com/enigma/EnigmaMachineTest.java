package com.enigma;

import com.enigma.core.EnigmaMachine;
import com.enigma.core.Plugboard;
import com.enigma.core.Reflector;
import com.enigma.core.ReflectorType;
import com.enigma.core.Rotor;
import com.enigma.core.RotorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 整机历史向量检验:全部期望值出自公开文献,是"与 1941 年实物行为一致"的硬证据
 *
 * @author echo
 * @date 2026-10-20
 */
class EnigmaMachineTest {

    /** 标准检验向量:Enigma I / I-II-III / AAA / 环AAA / B / 无插线,AAAAA → BDZGO */
    @Test
    @DisplayName("标准向量:AAAAA → BDZGO")
    void standardVectorBdzgo() {
        EnigmaMachine machine = buildDefault();
        assertEquals("BDZGO", typeWord(machine, "AAAAA"));
    }

    /** 环设置检验:同配置环改 BBB,AAAAA → EWTYX(环是电气上的隐形偏移) */
    @Test
    @DisplayName("环 BBB:AAAAA → EWTYX")
    void ringSettingBbbVector() {
        EnigmaMachine machine = new EnigmaMachine(
                new Rotor(RotorType.ROTOR_I, 0, 1),
                new Rotor(RotorType.ROTOR_II, 0, 1),
                new Rotor(RotorType.ROTOR_III, 0, 1),
                new Reflector(ReflectorType.REFLECTOR_B),
                new Plugboard(new int[][] {}));
        assertEquals("EWTYX", typeWord(machine, "AAAAA"));
    }

    /** 双步进异常:ADU 起步,窗口依次 ADV → AEW → BFX(中转子 D→E 后连跳 F) */
    @Test
    @DisplayName("双步进:ADU → ADV → AEW → BFX")
    void doubleSteppingAnomaly() {
        EnigmaMachine machine = new EnigmaMachine(
                new Rotor(RotorType.ROTOR_I, 0, 0),
                new Rotor(RotorType.ROTOR_II, 'D' - 'A', 0),
                new Rotor(RotorType.ROTOR_III, 'U' - 'A', 0),
                new Reflector(ReflectorType.REFLECTOR_B),
                new Plugboard(new int[][] {}));
        assertEquals("ADU", machine.rotorPositions());
        machine.pressKey('A');
        assertEquals("ADV", machine.rotorPositions());
        machine.pressKey('A');
        assertEquals("AEW", machine.rotorPositions());
        machine.pressKey('A');
        assertEquals("BFX", machine.rotorPositions());
    }

    /** 自逆性:同配置加密再"加密"还原明文(反射器带来的免模式切换) */
    @Test
    @DisplayName("同配置加解密互逆")
    void roundTripSelfInverse() {
        String plain = "ENIGMAMACHINEREVEALED";
        EnigmaMachine encryptor = buildDefault();
        String cipher = typeWord(encryptor, plain);
        EnigmaMachine decryptor = buildDefault();
        assertEquals(plain, typeWord(decryptor, cipher));
    }

    /** 报文密钥规程:QWE 下键入 RTZ 得 EWG(维基百科 Aachen 战报的头部) */
    @Test
    @DisplayName("报文头:QWE 下键入 RTZ → EWG")
    void messageKeyProcedure() {
        EnigmaMachine machine = new EnigmaMachine(
                new Rotor(RotorType.ROTOR_I, 'Q' - 'A', 'P' - 'A'),
                new Rotor(RotorType.ROTOR_IV, 'W' - 'A', 'Z' - 'A'),
                new Rotor(RotorType.ROTOR_III, 'E' - 'A', 'H' - 'A'),
                new Reflector(ReflectorType.REFLECTOR_B),
                new Plugboard(new int[][] {{0, 3}, {2, 13}, {4, 19}, {5, 11}, {6, 8},
                        {9, 21}, {10, 25}, {15, 20}, {16, 24}, {22, 23}}));
        assertEquals("EWG", typeWord(machine, "RTZ"));
    }

    /** 反射器无固定点:任何配置下密文字母恒不等于明文字母 */
    @Test
    @DisplayName("无固定点:密文字母永不等于对应明文字母")
    void noFixedPoint() {
        EnigmaMachine machine = buildDefault();
        String cipher = typeWord(machine, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        for (int i = 0; i < 26; i++) {
            org.junit.jupiter.api.Assertions.assertNotEquals(
                    (char) ('A' + i), cipher.charAt(i), "位置 " + i + " 出现固定点");
        }
    }

    /** 标准配置:转子 I-II-III / AAA / 环 AAA / 反射器 B / 无插线 */
    private EnigmaMachine buildDefault() {
        return new EnigmaMachine(
                new Rotor(RotorType.ROTOR_I, 0, 0),
                new Rotor(RotorType.ROTOR_II, 0, 0),
                new Rotor(RotorType.ROTOR_III, 0, 0),
                new Reflector(ReflectorType.REFLECTOR_B),
                new Plugboard(new int[][] {}));
    }

    /** 在机器上逐键敲入单词,返回灯板输出 */
    private String typeWord(EnigmaMachine machine, String word) {
        StringBuilder out = new StringBuilder(word.length());
        for (char c : word.toCharArray()) {
            out.append(machine.pressKey(c).lamp());
        }
        return out.toString();
    }
}
