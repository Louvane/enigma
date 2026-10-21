package com.enigma.server;

import com.enigma.EnigmaMachine;
import com.enigma.Plugboard;
import com.enigma.Reflector;
import com.enigma.ReflectorType;
import com.enigma.Rotor;
import com.enigma.RotorType;

/**
 * 恩尼格玛整机配置:对应操作员每日密钥单(转子序/初始位置/环设置/反射器/插线)
 *
 * @author echo
 * @date 2026-10-19
 */
public class MachineConfig {

    /** 转子序:下标 0/1/2 = 左/中/右槽位 */
    private final RotorType[] rotorOrder;
    /** 初始位置:三槽各自 0-25 */
    private final int[] positions;
    /** 环设置:三槽各自 0-25 */
    private final int[] ringSettings;
    /** 反射器型号 */
    private final ReflectorType reflectorType;
    /** 插线对:每对为 [字母A索引, 字母B索引] */
    private final int[][] plugPairs;

    /**
     * 全参构造,字段不可变;解析校验由调用方(EnigmaServer)完成
     *
     * @param rotorOrder    转子序(左中右)
     * @param positions     初始位置
     * @param ringSettings  环设置
     * @param reflectorType 反射器型号
     * @param plugPairs     插线对
     */
    public MachineConfig(RotorType[] rotorOrder, int[] positions, int[] ringSettings,
                         ReflectorType reflectorType, int[][] plugPairs) {
        this.rotorOrder = rotorOrder;
        this.positions = positions;
        this.ringSettings = ringSettings;
        this.reflectorType = reflectorType;
        this.plugPairs = plugPairs;
    }

    /**
     * 按当前配置新装一台整机(转子位置回到初始设定)
     *
     * @return 新 EnigmaMachine 实例
     */
    public EnigmaMachine buildMachine() {
        Rotor left = new Rotor(rotorOrder[0], positions[0], ringSettings[0]);
        Rotor middle = new Rotor(rotorOrder[1], positions[1], ringSettings[1]);
        Rotor right = new Rotor(rotorOrder[2], positions[2], ringSettings[2]);
        Reflector reflector = new Reflector(reflectorType);
        Plugboard plugboard = new Plugboard(plugPairs);
        return new EnigmaMachine(left, middle, right, reflector, plugboard);
    }

    /**
     * 转子序(左中右)
     *
     * @return 转子型号数组
     */
    public RotorType[] getRotorOrder() {
        return rotorOrder;
    }

    /**
     * 初始位置索引
     *
     * @return 三槽位置
     */
    public int[] getPositions() {
        return positions;
    }

    /**
     * 环设置索引
     *
     * @return 三槽环设置
     */
    public int[] getRingSettings() {
        return ringSettings;
    }

    /**
     * 反射器型号
     *
     * @return 型号枚举
     */
    public ReflectorType getReflectorType() {
        return reflectorType;
    }

    /**
     * 插线对
     *
     * @return 插线对数组
     */
    public int[][] getPlugPairs() {
        return plugPairs;
    }
}
