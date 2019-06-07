package uk.co.xsc.securitymod.util;

public class ArrayUtil {

    public static int[] intFromStr(String[] strArr, boolean allowXWildcard, boolean suppressErr) {
        int[] intArr = new int[strArr.length];
        if (strArr.length > 0) {
            for (int i = 0; i < strArr.length; i++) {
                try {
                    intArr[i] = Integer.parseInt(strArr[i]);
                } catch (NumberFormatException ex) {
                    if (strArr[i].equalsIgnoreCase("x") && allowXWildcard) {
                        intArr[i] = 0x784;
                        continue;
                    }
                    if (strArr[i].length() == 0) {
                        intArr[i] = 0x785;
                        continue;
                    }
                    if (!suppressErr) System.out.println("Bad input! Skipping... (Value of 0 will be assigned)");
                    intArr[i] = 0;
                }
            }
        }
        return intArr;
    }

    public static int[] intFromStr(String[] strArr, boolean allowXWildcard) {
        return intFromStr(strArr, allowXWildcard, false);
    }

    public static int[] intFromStr(String[] strArr) {
        return intFromStr(strArr, false, false);
    }

}
