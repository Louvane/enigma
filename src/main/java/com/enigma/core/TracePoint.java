package com.enigma.core;

/**
 * 信号路径追踪点:记录信号经过某个物理组件后的字母
 *
 * @author echo
 * @date 2026-10-19
 */
/**
 * 信号路径追踪点:记录信号经过某个物理组件后的字母
 *
 * @param stage  组件站名(键盘/插线板/右转子/反射器/灯板等)
 * @param letter 经过该组件后的字母
 */
public record TracePoint(String stage, char letter) {

    @Override
    public String toString() {
        return stage + ":" + letter;
    }
}
