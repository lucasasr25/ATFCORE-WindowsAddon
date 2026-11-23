package com.winlator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.RootFSInstaller;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

/**
 * Entry point for the ATFCORE player (protected by a signature permission): opens a Windows
 * executable in the add-on's default container.
 *
 * Intent extras:
 *   exec_path  absolute path of the .exe on shared storage (required)
 *   title      game title (optional, for messages)
 *
 * Result: RESULT_OK when the game was started, RESULT_CANCELED with an "error" extra otherwise.
 */
public class ExternalLaunchActivity extends Activity {
    public static final String EXTRA_EXEC_PATH = "exec_path";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ERROR = "error";
    private static final String DEFAULT_CONTAINER_NAME = "ATFCORE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String execPath = getIntent().getStringExtra(EXTRA_EXEC_PATH);
        if (execPath == null || !new File(execPath).isFile()) { fail(getString(R.string.atfcore_missing_executable)); return; }

        // First run: the Linux userland is installed by the main screen (it shows the progress).
        RootFS rootFS = RootFS.find(this);
        if (!rootFS.isValid() || rootFS.getVersion() < RootFSInstaller.LATEST_VERSION) {
            Toast.makeText(this, R.string.atfcore_setup_first, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            fail(getString(R.string.atfcore_setup_first));
            return;
        }

        ContainerManager manager = new ContainerManager(this);
        ArrayList<Container> containers = manager.getContainers();
        Container existing = null;
        for (Container c : containers) if (DEFAULT_CONTAINER_NAME.equals(c.getName())) existing = c;
        if (existing == null && !containers.isEmpty()) existing = containers.get(0);
        if (existing != null) { launch(existing, execPath); return; }

        JSONObject data = new JSONObject();
        try { data.put("name", DEFAULT_CONTAINER_NAME); } catch (JSONException e) { fail(e.toString()); return; }
        manager.createContainerAsync(data, container -> runOnUiThread(() -> {
            if (container == null) fail(getString(R.string.atfcore_container_failed));
            else launch(container, execPath);
        }));
    }

    private void launch(Container container, String execPath) {
        Intent intent = new Intent(this, XServerDisplayActivity.class);
        intent.putExtra("container_id", container.id);
        intent.putExtra("exec_path", execPath);
        startActivity(intent);
        setResult(RESULT_OK);
        finish();
    }

    private void fail(String message) {
        Intent result = new Intent();
        result.putExtra(EXTRA_ERROR, message);
        setResult(RESULT_CANCELED, result);
        finish();
    }
}
