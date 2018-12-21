import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.IntByReference;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class JNATestApplication {

    private static User32 user32;// = User32.INSTANCE;
    private static Set<Integer> set;//=new HashSet<Integer>();
    private static IntByReference i;//=new IntByReference();//放PID

    public static ArrayList<WinDef.HWND> hTargetWindow = new ArrayList<WinDef.HWND>();
    private static char[] chText = new char[1024];

    public static Set<Integer> getTaskPID() {
        user32 = User32.INSTANCE;
        set = new HashSet<Integer>();
        i = new IntByReference();//放PID
        boolean b = user32.EnumWindows(new WNDENUMPROC() {
            public boolean callback(WinDef.HWND h, Pointer p) {
                user32.GetWindowThreadProcessId(h, i);//获取窗口的PID
                if (user32.IsWindow(h) && user32.IsWindowEnabled(h) && user32.IsWindowVisible(h)) {
                    set.add(i.getValue());
                }
                char[] chWinText = new char[user32.GetWindowTextLength(h) + 1];
                user32.GetWindowText(h, chWinText, chWinText.length);
                System.out.println(chWinText);
                return true;
            }
        }, null);
        return set;
    }

    public static ArrayList<WinDef.HWND> FindWindowOnTop(final String strWindowName) {
        user32 = User32.INSTANCE;
        final ArrayList<WinDef.HWND> lHandles = new ArrayList<WinDef.HWND>();
        boolean b = user32.EnumWindows(new WNDENUMPROC() {
            public boolean callback(WinDef.HWND h, Pointer p) {
                char[] chWinText = new char[user32.GetWindowTextLength(h) + 1];
                if (1 == chWinText.length)
                    return true;
                user32.GetWindowText(h, chWinText, chWinText.length);
                if (String.valueOf(chWinText).contains(strWindowName)) {
                    lHandles.add(h);
                    System.out.println(chWinText);
                }
                return true;
            }
        }, null);
        return lHandles;
    }

//    public static ArrayList<WinDef.HWND> FindWindowInChildren(WinDef.HWND hFatherWindow, final String strWindowName, final String strClassName) {
//
//    }

    public static boolean FindTargetWindow(WinDef.HWND hRootWindow, final  String strTargetWindowName, final String strTargetClassName) {
        user32 = User32.INSTANCE;
        boolean b = user32.EnumChildWindows(hRootWindow, new WNDENUMPROC() {
            public boolean callback(WinDef.HWND h, Pointer p) {
                SetCharArrayValue(chText, 0);
                user32.GetWindowText(h, chText, chText.length);
                String strWinTitle = CharArrayToString(chText);
                SetCharArrayValue(chText, 0);
                user32.GetClassName(h, chText, chText.length);
                String strClassName = CharArrayToString(chText);
                if ((strWinTitle.compareTo(strTargetWindowName) == 0 || strWinTitle.contains(strTargetClassName)) && (strClassName.compareTo(strTargetClassName) == 0 || strClassName.contains(strTargetClassName))) {
                    hTargetWindow.add(h);
                }
                System.out.println(strWinTitle);
                WinDef.DWORDByReference buf = JNATestApplication.GetWindowText(h);
                Pointer pointer = buf.getPointer();
                char[] ret = JNATestApplication.ReadPointer(pointer);
                System.out.format("%s: %s\n", h.toString(), String.valueOf(ret));

                return true;
            }
        }, null);

        return true;
    }

    public static WinDef.DWORDByReference GetWindowText(WinDef.HWND h) {
        WinDef.DWORD value = new WinDef.DWORD(0);
        WinDef.DWORDByReference pointer = new WinDef.DWORDByReference(value);
        user32 = User32.INSTANCE;
        user32.SendMessageTimeout(h, 1026, null, null, WinUser.SMTO_ABORTIFHUNG, 1000, pointer);
        return pointer;
    }

    public static void Keybord(WinDef.HWND h) {
        WinUser.INPUT input = new WinUser.INPUT( );
        input.type = new WinDef.DWORD( WinUser.INPUT.INPUT_KEYBOARD );
        input.input.setType("ki");//Because setting INPUT_INPUT_KEYBOARD is not enough: https://groups.google.com/d/msg/jna-users/NDBGwC1VZbU/cjYCQ1CjBwAJ
        input.input.ki.wScan = new WinDef.WORD( 0 );
        input.input.ki.time = new WinDef.DWORD( 0 );
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR( 0 );

        //Press"a"
        input.input.ki.wVk = new WinDef.WORD( 'A' );//0x41
        input.input.ki.dwFlags = new WinDef.DWORD( 0 );//keydown
        User32.INSTANCE.SendInput( new WinDef.DWORD( 1 ), ( WinUser.INPUT[] ) input.toArray( 1 ), input.size() );
        //Release"a"
        input.input.ki.wVk = new WinDef.WORD( 'A' );//0x41
        input.input.ki.dwFlags = new WinDef.DWORD( 2 );//keyup
        User32.INSTANCE.SendInput( new WinDef.DWORD( 1 ), ( WinUser.INPUT[] ) input.toArray( 1 ), input.size() );

    }

    private static void SetCharArrayValue(char[] ch, int iVal) {
        for (int i1 = 0; i1 < ch.length; i1++) {
            ch[i1] = (char)iVal;
        }
    }

    private static String CharArrayToString(char[] ch) {
        if (ch[0] == 0)
            return "";

        int iCount = 0;
        String str = String.valueOf(ch);
        for (int i1 = 0; i1 < ch.length; i1++) {
            if (ch[i1] != (char)0)
                iCount++;
            else
                break;
        }

        return str.substring(0, iCount);
    }

    public static char[] ReadPointer(Pointer poi) {
        char[] ch = new char[50];
        for (int j = 0; j < 50; j++) {
            try{
                ch[j] = poi.getChar(j * 2);
            }
            catch (Exception e) {
                break;
            }
        }

        for (int j = 0; j < 50; j++) {
            if (ch[j] == 0)
                ch[j] = '.';
        }

        return ch;
    }

    public static void main(String[] args) {
        ArrayList<WinDef.HWND> lHandles = JNATestApplication.FindWindowOnTop("国金太阳至强版");
        //ArrayList<WinDef.HWND> lHandles = JNATestApplication.FindWindowOnTop("记事本");
        User32.INSTANCE.SetForegroundWindow(lHandles.get(0));
        User32.INSTANCE.SetFocus(lHandles.get(0));
        System.out.println("find top window: " + lHandles.size());
        JNATestApplication.FindTargetWindow(lHandles.get(0), "", "RICHEDIT");
        System.out.println("find target window: " + JNATestApplication.hTargetWindow.size());
        WinDef.DWORDByReference buf;
        if (JNATestApplication.hTargetWindow.size() > 0) {
            buf = JNATestApplication.GetWindowText(JNATestApplication.hTargetWindow.get(0));
            Pointer pointer = buf.getPointer();
            char[] ret = JNATestApplication.ReadPointer(pointer);
            System.out.println(ret);
        }
    }
}
