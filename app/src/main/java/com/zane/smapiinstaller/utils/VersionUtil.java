package com.zane.smapiinstaller.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;

public class VersionUtil {
    private static int compareVersionSection(String sectionA, String sectionB) {
        try {
            return Integer.compare(Integer.parseInt(sectionA), Integer.parseInt(sectionB));
        } catch (Exception ignored) {
        }
        List<String> listA = Splitter.on("-").splitToList(sectionA);
        List<String> listB = Splitter.on("-").splitToList(sectionB);
        int i;
        for (i = 0; i < listA.size() && i < listB.size(); i++) {
            Integer intA = null;
            Integer intB = null;
            try {
                intA = Integer.parseInt(listA.get(i));
                return Integer.compare(intA, Integer.parseInt(listB.get(i)));
            } catch (Exception ignored) {
                try {
                    intB = Integer.parseInt(listB.get(i));
                } catch (Exception ignored2) {
                }
            }
            if(StringUtils.equals(listA.get(i), listB.get(i)))
                continue;
            if(intA != null && intB == null)
                return 1;
            else if(intA == null)
                return -1;
            return listA.get(i).compareTo(listB.get(i));
        }
        return Integer.compare(listA.size(), listB.size());
    }
    private static boolean isZero(List<String> versionSections) {
        return !Iterables.filter(versionSections, version -> {
            try {
                int i = Integer.parseInt(version);
                if (i == 0) {
                    return false;
                }
            } catch (Exception ignored) {
            }
            return true;
        }).iterator().hasNext();
    }

    public static int compareVersion(String versionA, String versionB) {
        List<String> versionSectionsA = Splitter.on(".").splitToList(versionA);
        List<String> versionSectionsB = Splitter.on(".").splitToList(versionB);
        for (int i = 0; i < versionSectionsA.size(); i++) {
            if (versionSectionsB.size() <= i) {
                if(isZero(versionSectionsA.subList(i, versionSectionsA.size()))) {
                    return 0;
                }
                return 1;
            }
            int compare = compareVersionSection(versionSectionsA.get(i), versionSectionsB.get(i));
            if(compare != 0)
                return compare;
        }
        if(versionSectionsA.size() < versionSectionsB.size()) {
            if(isZero(versionSectionsB.subList(versionSectionsA.size(), versionSectionsB.size()))) {
                return 0;
            }
            return -1;
        }
        return 0;
    }
}
