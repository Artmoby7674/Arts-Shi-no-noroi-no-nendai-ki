package com.shinoroi.data;

/**
 * Classifies a technique by its resource cost and power tier.
 *
 * <ul>
 *   <li>NORMAL  – uses per-technique cooldown, no ult bar cost.</li>
 *   <li>SECRET  – costs ult bar charge.</li>
 *   <li>ULT     – full ult bar required; fires cinematic trigger.</li>
 * </ul>
 */
public enum TechniqueType {
    NORMAL,
    SECRET,
    ULT
}
