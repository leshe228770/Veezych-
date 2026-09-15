package ru.meow.module.impl.combat;

/** One selectable threat source for {@link AutoTotem}. */
interface AutoTotemThreatChecker {

    String getKey();

    boolean check(AutoTotem module);
}
