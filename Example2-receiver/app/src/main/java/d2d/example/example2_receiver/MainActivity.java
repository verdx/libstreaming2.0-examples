package d2d.example.example2_receiver;

import android.os.Bundle;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.verdx.libstreaming.BasicViewModel;
import net.verdx.libstreaming.DefaultViewModel;
import net.verdx.libstreaming.StreamListAdapter;
import net.verdx.libstreaming.Streaming;
import net.verdx.libstreaming.StreamingRecord;
import net.verdx.libstreaming.StreamingRecordObserver;
import net.verdx.libstreaming.gui.StreamDetail;
import net.verdx.libstreaming.sessions.SessionBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * A straightforward example of how to stream AMR and H.263 to some public IP using libstreaming.
 * Note that this example may not be using the latest version of libstreaming !
 */
public class MainActivity extends AppCompatActivity implements StreamingRecordObserver, SwipeRefreshLayout.OnRefreshListener {

    private final static String TAG = "MainActivity";
    private ArrayList<StreamDetail> streamList;
    private StreamListAdapter arrayAdapter;
    private BasicViewModel mViewModel;
    private EditText mIncomingIpsEditText;
    private TextView mStatusTextView;
    private SwipeRefreshLayout mArrayListRefresh;
    private Boolean isNetworkAvailable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        streamList = new ArrayList<>();
        mViewModel = new DefaultViewModel(this.getApplication());
        mStatusTextView = findViewById(R.id.statusTextView);
        mIncomingIpsEditText = findViewById(R.id.editTextIncomingIP);
        setIncomingIps();

        mArrayListRefresh = findViewById(R.id.swiperefresh);
        mArrayListRefresh.setOnRefreshListener(this);

        RecyclerView streamsListView = this.findViewById(R.id.streamsList);
        streamsListView.setLayoutManager(new LinearLayoutManager(this));
        addDefaultItemList();
        arrayAdapter = new StreamListAdapter(this, streamList, this);
        streamsListView.setAdapter(arrayAdapter);

        StreamingRecord.getInstance().addObserver(this);

        mViewModel.isNetworkAvailable().observe(this, (Observer<Boolean>) aBoolean -> {
            isNetworkAvailable = aBoolean;
            mStatusTextView.setText(getDeviceStatus());
            if(isNetworkAvailable){
                mViewModel.initNetwork();
            }
        });
    }

    private void setIncomingIps() {
        String[] ipArray = mIncomingIpsEditText.getText().toString().replaceAll("\\s","").split(",");
        ArrayList<String> ipList = new ArrayList<>();
        Collections.addAll(ipList, ipArray);
        ((DefaultViewModel)mViewModel).setDestinationIpsArray(ipList);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        StreamingRecord.getInstance().removeObserver(this);
    }

    @Override
    public void onLocalStreamingAvailable(UUID id, String name, SessionBuilder sessionBuilder) {}

    @Override
    public void onLocalStreamingUnavailable() {}

    @Override
    public void onStreamingAvailable(Streaming streaming, boolean bAllowDispatch) {
        final String path = streaming.getUUID().toString();
        this.runOnUiThread(() -> updateList(true,
                path,
                streaming.getName(),
                streaming.getReceiveSession().getDestinationAddress().toString(),
                streaming.getReceiveSession().getDestinationPort(),
                streaming.isDownloading()));
    }

    @Override
    public void onStreamingUnavailable(Streaming streaming) {
        final String path = streaming.getUUID().toString();
        this.runOnUiThread(() -> updateList(false,
                path,
                streaming.getName(),
                streaming.getReceiveSession().getDestinationAddress().toString(),
                streaming.getReceiveSession().getDestinationPort(),
                streaming.isDownloading()));
    }

    @Override
    public void onStreamingDownloadStateChanged(Streaming streaming, boolean bIsDownloading) {
        final String path = streaming.getUUID().toString();
        this.runOnUiThread(() -> setStreamDownload(path, bIsDownloading));
    }

    @Override
    public void onRefresh() {
        setIncomingIps();
        if (isNetworkAvailable) mViewModel.initNetwork();
        mArrayListRefresh.setRefreshing(false);
    }

    private void addDefaultItemList(){
        streamList.clear();
        streamList.add(null);
        streamList.add(null);
        streamList.add(null);
    }

    private void removeDefaultItemList(){
        streamList.removeIf(Objects::isNull);
    }


    public String getDeviceStatus() {
        Pair<Boolean, String> status = mViewModel.getDeviceStatus(this);
        if(status.first){
            mStatusTextView.setTextColor(getResources().getColor(net.verdx.libstreaming.R.color.colorAccent, null));
            return status.second;
        }

        mStatusTextView.setTextColor(getResources().getColor(net.verdx.libstreaming.R.color.colorRed, null));
        return status.second;

    }

    public void updateList(boolean on_off, String uuid, String name, String ip, int port, boolean download){
        removeDefaultItemList();
        if(!ip.equals("0.0.0.0")) {
            StreamDetail detail = new StreamDetail(uuid, name, ip, port, download);
            if (on_off) {
                if (!streamList.contains(detail))
                    streamList.add(detail);
            } else {
                streamList.remove(detail);
            }
            if(streamList.size() == 0) addDefaultItemList();
            arrayAdapter.setStreamsData(streamList);
        }
    }

    public void setStreamDownload(String uuid, boolean isDownload){
        for(StreamDetail value: streamList){
            if (value.getUuid().equals(uuid)) {
                value.setDownload(isDownload);
                arrayAdapter.setStreamsData(streamList);
                return;
            }
        }
    }
}
