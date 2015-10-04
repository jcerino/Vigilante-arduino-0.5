package com.example.android.vigilantearduino;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;

public class Inicio extends AppCompatActivity implements View.OnClickListener {
    Boolean enRango=true;
    BluetoothDevice dispositivo=null;
    private BluetoothAdapter myBluetooth = null;
    private Set pairedDevices;
    String address = null;
    BluetoothSocket btSocket = null;
    ImageView imagen=null;
    EditText txtOrden=null;
    TextView lblDispositivo=null;
    private Integer Tope=70;

    private void FijarImagen(boolean estatus){

        if(estatus==false)
            imagen.setImageResource(R.drawable.incorrecto);
        else
            imagen.setImageResource(R.drawable.correcto);
        imagen.setTag(estatus);
    }
    private void IniciarObjetos(){
        lblDispositivo=(TextView) findViewById(R.id.lblDispositivo);
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        Button btn=(Button) findViewById(R.id.btnCambia);
        Button btnEnviar=(Button) findViewById(R.id.btnEnviar);
        Button btnRSSI=(Button) findViewById(R.id.btnRSSI);
        txtOrden=(EditText) findViewById(R.id.txtOrden);
        imagen=(ImageView) findViewById(R.id.imgAlerta);
        imagen.setTag(true);
        btn.setOnClickListener(this);
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        btnRSSI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myBluetooth.startDiscovery();
                if(address!=null) {
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    lblDispositivo.setText(dispositivo.getName());
                }else
                    lblDispositivo.setText("No conectado");
                FijarImagen(address!=null);

            }
        });
        btnEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    btSocket =dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    btSocket.connect();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "No se logró conectar al dispositivo", Toast.LENGTH_LONG).show();
                }
//Enviar el valor
                if (btSocket != null) {
                    try {
                        btSocket.getOutputStream().write(txtOrden.getText().toString().getBytes());
                        Toast.makeText(getApplicationContext(), "Valor enviado", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Error al enviar el valor", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

    }
    private void IniciarNotiManejador(){
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        final AsyncJob.OnBackgroundJob job = new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {
                // This toast should show a difference of 1000ms between calls
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                    @Override
                    public void doInUIThread() {
                            adapter.startDiscovery();
                        //Toast.makeText(getApplicationContext(), "Finished on: "+System.currentTimeMillis(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {
            @Override
            public void doOnBackground() {

                while (true) {
                    try {
                        if (adapter != null && !adapter.isDiscovering()) {
                            AsyncJob.doInBackground(job, executorService);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });

    }
    static final UUID myUUID = UUID.fromString("ff375ad4-a157-495b-9ec8-7ae9b71a4174");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        IniciarObjetos();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.equals(dispositivo)) {
                    myBluetooth.cancelDiscovery();
                    String dispotivo = device.getAddress();
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)*-1;
                    String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    TextView rssi_msg = (TextView) findViewById(R.id.txtOrden);
                    rssi_msg.setText(rssi + "dBm");
                    enRango=rssi < Tope;
                    FijarImagen(enRango);
                    lblDispositivo.setText(enRango?"Conectado":"Desconectado");
                }
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void  onClick(View v){
        Context ctx=this.getApplicationContext();

        Toast.makeText(getApplicationContext(),imagen.getDrawable().getConstantState().toString(),Toast.LENGTH_LONG).show();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_inicio, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, DeviceList.class);
            startActivityForResult(i, 1);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if ((requestCode == 1) && (resultCode == RESULT_OK)){
            String naddress=data.getStringExtra("device_address");
            Boolean cambioDir=address!=naddress;
            Toast.makeText(this.getApplicationContext(), data.getStringExtra("device_address"),Toast.LENGTH_LONG).show();
            //Aqui se crea la conexion al bluetooth
    if(cambioDir) {
        address=naddress;
        dispositivo = myBluetooth.getRemoteDevice(address);
        try {
            btSocket =dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"No se logró crear el socket",Toast.LENGTH_LONG).show();
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        try {

            btSocket.connect();
            Toast.makeText(getApplicationContext(),"1.-Se ha conectado al dispositivo con exito",Toast.LENGTH_LONG).show();
            btSocket.close();
            IniciarNotiManejador();
        } catch (IOException e) {

            try {
                Log.e("","trying fallback...");
                btSocket=(BluetoothSocket) dispositivo.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(dispositivo,1);
                btSocket.connect();
                Toast.makeText(getApplicationContext(),"2.-Se ha conectado al dispositivo con exito",Toast.LENGTH_LONG).show();
                btSocket.close();
                IniciarNotiManejador();

            }
            catch (Exception e2) {
                Log.e("", "Couldn't establish Bluetooth connection!");
                Toast.makeText(getApplicationContext(),"No se logró la conexion:"+e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }



    }

            }
        }

    @Override
    public void onPause() {
        super.onPause();
        if(btSocket!=null )
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(btSocket!=null )
            try {
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
    }

