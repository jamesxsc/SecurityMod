package uk.co.xsc.securitymod.util;

public class ArrayUtil {

    public static int[] intFromStr(String[] strArr, boolean allowXWildcard) {
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
                    System.out.println("Bad input! Skipping... (Value of 0 will be assigned)");
                    intArr[i] = 0;
                }
            }
        }
        return intArr;
    }

}
