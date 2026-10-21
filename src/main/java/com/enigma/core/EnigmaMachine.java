package com.enigma.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 恩尼格玛整机
 * 复原物理信号路径:按键 → 插线板 → 入口轮(ETW,Enigma I 恒等映射)→
 * 右转子 → 中转子 → 左转子(正向)→ 反射器 → 左 → 中 → 右(反向)→
 * 插线板 → 灯板。
 * 转子步进含真实机械的"双步进异常":中转子停在自身缺口时,下一键
 * 自身步进并带动左转子,即使右转子未到缺口。
 *
 * @author echo
 * @date 2026-10-19
 */
public class EnigmaMachine {

    /** 左转子(电流第三站,离反射器最近) */
    private final Rotor leftRotor;
    /** 中转子(带缺口拨杆,存在双步进异常) */
    private final Rotor middleRotor;
    /** 右转子(每键步进一格) */
    private final Rotor rightRotor;
    /** 反射器(两两配对,信号在此折返,自逆的来源) */
    private final Reflector reflector;
    /** 插线板(信号出、入各经过一次) */
    private final Plugboard plugboard;

    /**
     * 整机装配:转子按电流方向传入(左/中/右),全部成员不可变
     *
     * @param leftRotor   左转子
     * @param middleRotor 中转子
     * @param rightRotor  右转子(每键步进)
     * @param reflector   反射器
     * @param plugboard   插线板
     */
    public EnigmaMachine(Rotor leftRotor, Rotor middleRotor, Rotor rightRotor,
                         Reflector reflector, Plugboard plugboard) {
        this.leftRotor = leftRotor;
        this.middleRotor = middleRotor;
        this.rightRotor = rightRotor;
        this.reflector = reflector;
        this.plugboard = plugboard;
    }

    /**
     * 按下一个键:先步进转子(通电前的机械联动),再走信号路径。
     * 因反射器自逆,加密与解密共用本方法,无需模式开关
     *
     * @param letter 输入字母 A-Z
     * @return 灯板结果与信号路径
     */
    public synchronized KeyPressResult pressKey(char letter) {
        char upper = Character.toUpperCase(letter);
        if (upper < 'A' || upper > 'Z') {
            throw new IllegalArgumentException("非法按键:" + letter);
        }
        stepRotors();

        List<TracePoint> path = new ArrayList<>(12);
        int pin = upper - 'A';
        path.add(new TracePoint("键盘", upper));

        // 出口方向:先插线板
        pin = plugboard.swap(pin);
        path.add(new TracePoint("插线板", (char) ('A' + pin)));

        // 入口轮 ETW:Enigma I 接线为恒等映射,信号不变,仅教学记录
        path.add(new TracePoint("入口轮ETW", (char) ('A' + pin)));

        // 正向穿过三个转子(右 → 中 → 左)
        pin = rightRotor.forwardPass(pin);
        path.add(new TracePoint("右转子", (char) ('A' + pin)));
        pin = middleRotor.forwardPass(pin);
        path.add(new TracePoint("中转子", (char) ('A' + pin)));
        pin = leftRotor.forwardPass(pin);
        path.add(new TracePoint("左转子", (char) ('A' + pin)));

        // 反射器折返
        pin = reflector.reflect(pin);
        path.add(new TracePoint("反射器", (char) ('A' + pin)));

        // 反向穿过三个转子(左 → 中 → 右)
        pin = leftRotor.backwardPass(pin);
        path.add(new TracePoint("左转子⁻¹", (char) ('A' + pin)));
        pin = middleRotor.backwardPass(pin);
        path.add(new TracePoint("中转子⁻¹", (char) ('A' + pin)));
        pin = rightRotor.backwardPass(pin);
        path.add(new TracePoint("右转子⁻¹", (char) ('A' + pin)));

        // 回程再过插线板,灯板点亮
        pin = plugboard.swap(pin);
        char lamp = (char) ('A' + pin);
        path.add(new TracePoint("插线板", lamp));
        path.add(new TracePoint("灯板", lamp));

        return new KeyPressResult(lamp, rotorPositions(), path);
    }

    /**
     * 转子步进,发生在通电之前(按键的机械联动)
     *
     * 常规进位:右转子停在缺口 → 本键带动中转子 +1;
     * 双步进异常:中转子自己停在缺口 → 本键中转子 +1 且带动左转子 +1,
     * 与右转子是否到缺口无关。机械成因:中转子缺口拨杆与左转子咬合
     * 成一体,拨杆一弹同时推两盘,于是中转子连跳两格。
     * 许多模拟器在此出错;标准验证序列见 EnigmaSelfTest#testDoubleStepping
     */
    private void stepRotors() {
        if (middleRotor.atNotch()) {
            middleRotor.step();
            leftRotor.step();
        } else if (rightRotor.atNotch()) {
            middleRotor.step();
        }
        rightRotor.step();
    }

    /**
     * 当前三转子角位置,如 "ABL"
     *
     * @return 左中右位置字母拼接
     */
    public synchronized String rotorPositions() {
        return "" + leftRotor.positionLetter() + middleRotor.positionLetter() + rightRotor.positionLetter();
    }

    /**
     * 左转子只读视图(用于前端状态回显)
     *
     * @return 左转子
     */
    public synchronized Rotor getLeftRotor() {
        return leftRotor;
    }

    /**
     * 中转子只读视图
     *
     * @return 中转子
     */
    public synchronized Rotor getMiddleRotor() {
        return middleRotor;
    }

    /**
     * 右转子只读视图
     *
     * @return 右转子
     */
    public synchronized Rotor getRightRotor() {
        return rightRotor;
    }

    /**
     * 反射器只读视图
     *
     * @return 反射器
     */
    public synchronized Reflector getReflector() {
        return reflector;
    }

    /**
     * 插线板只读视图
     *
     * @return 插线板
     */
    public synchronized Plugboard getPlugboard() {
        return plugboard;
    }
}
