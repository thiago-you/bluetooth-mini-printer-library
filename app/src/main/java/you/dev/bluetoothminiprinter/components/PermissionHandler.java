package you.dev.bluetoothminiprinter.components;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHandler {

    public static final int PERMISSION_REQUEST_CODE = 99;

    public static void requestDefaultPermissions(Activity activity) {
        List<String> permissionsToRequest;

        /* check permission list */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /* set permission list to request */
            String[] permissionList = new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };

            /* validate and get not allowed permissions */
            permissionsToRequest = PermissionHandler.getNotAllowedList(activity, permissionList);

            /* request permissions */
            if (permissionsToRequest.size() > 0) {
                /* request permission list */
                PermissionHandler.requestPermissions(activity, permissionsToRequest);
            }
        }
    }

    private static List<String> getNotAllowedList(Activity activity, String[] permissionList) {
        List<String> requestList = new ArrayList<>();

        if (permissionList.length > 0) {
            for (String permission : permissionList) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    requestList.add(permission);
                }
            }
        }

        return requestList;
    }

    private static void requestPermissions(Activity activity, List<String> permissions) {
        if (permissions.size() > 0) {
            /* set permission array list to request */
            String[] array = new String[permissions.size()];
            for (int i = 0; i < permissions.size(); i++) {
                array[i] = permissions.get(i);
            }

            /* request permission */
            ActivityCompat.requestPermissions(activity, array, PermissionHandler.PERMISSION_REQUEST_CODE);
        }
    }
}
