/*
 * Copyright (C) 2010, 2011, 2012 Herbert von Broeuschmeul
 * Copyright (C) 2010, 2011, 2012 BluetoothGPS4Droid Project
 * Copyright (C) 2011, 2012 UsbGPS4Droid Project
 * 
 * This file is part of UsbGPS4Droid.
 *
 * UsbGPS4Droid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * UsbGPS4Droid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with UsbGPS4Droid. If not, see <http://www.gnu.org/licenses/>.
 */

package org.broeuschmeul.android.gps.bluetooth.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.broeuschmeul.android.gps.usb.provider.R;
import org.broeuschmeul.android.gps.nmea.util.NmeaParser;
import org.broeuschmeul.android.gps.sirf.util.SirfUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.location.LocationManager;
import android.location.GpsStatus.NmeaListener;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.os.SystemClock;
import android.util.Log;

/**
 * This class is used to establish and manage the connection with the bluetooth GPS.
 * 
 * @author Herbert von Broeuschmeul
 *
 */
public class BlueetoothGpsManager {

	/**
	 * Tag used for log messages
	 */
	private static final String LOG_TAG = "UsbGPS";

	/**
	 * A utility class used to manage the communication with the bluetooth GPS whn the connection has been established.
	 * It is used to read NMEA data from the GPS or to send SIRF III binary commands or SIRF III NMEA commands to the GPS.
	 * You should run the main read loop in one thread and send the commands in a separate one.   
	 * 
	 * @author Herbert von Broeuschmeul
	 *
	 */
	private class ConnectedGps extends Thread {
		/**
		 * GPS bluetooth socket used for communication. 
		 */
		private final File gpsDev ;
		/**
		 * GPS InputStream from which we read data. 
		 */
		private final InputStream in;
		/**
		 * GPS output stream to which we send data (SIRF III binary commands). 
		 */
		//private final OutputStream out;
		/**
		 * GPS output stream to which we send data (SIRF III NMEA commands). 
		 */
		//private final PrintStream out2;
		/**
		 * A boolean which indicates if the GPS is ready to receive data. 
		 * In fact we consider that the GPS is ready when it begins to sends data...
		 */
		private boolean ready = false;

		public ConnectedGps(File gpsDev) {
			this.gpsDev = gpsDev;
			InputStream tmpIn = null;
			//OutputStream tmpOut = null;
			//PrintStream tmpOut2 = null;
			try {
				tmpIn = new FileInputStream(gpsDev);
				//tmpOut = new FileOutputStream(gpsDev);
				//if (tmpOut != null){
				//	tmpOut2 = new PrintStream(tmpOut, false, "US-ASCII");
				//}
			} catch (IOException e) {
				Log.e(LOG_TAG, "error while getting socket streams", e);
			}	
			in = tmpIn;
			//out = tmpOut;
			//out2 = tmpOut2;
		}
	
		public boolean isReady(){
			return ready;
		}
		
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in,"US-ASCII"));
				String s;
				long now = SystemClock.uptimeMillis();
				long lastRead = now;
				while((enabled) && (now < lastRead+30000 )){
					//if (reader.ready()){
					if ((s = reader.readLine()) != null){
						Log.v(LOG_TAG, "data: "+System.currentTimeMillis()+" "+s);
						notifyNmeaSentence(s);
						ready = true;
						lastRead = SystemClock.uptimeMillis();
					} else {
						Log.d(LOG_TAG, "data: not ready "+System.currentTimeMillis());
						SystemClock.sleep(500);
					}
					now = SystemClock.uptimeMillis();
				}
			} catch (IOException e) {
				Log.e(LOG_TAG, "error while getting data: " + e.getMessage(), e);
				setMockLocationProviderOutOfService();
			} catch (Exception e) {
				Log.e(LOG_TAG, "error while getting data: " + e.getMessage(), e);
				setMockLocationProviderOutOfService();
			} finally {
				// cleanly closing everything...
				this.close();
				disableIfNeeded();
			}
		}

		/**
		 * Write to the connected OutStream.
		 * @param buffer  The bytes to write
		 */
//		public void write(byte[] buffer) {
//			try {
//				do {
//					Thread.sleep(100);
//				} while ((enabled) && (! ready));
//				if ((enabled) && (ready)){
//					out.write(buffer);
//					out.flush();
//				}
//			} catch (IOException e) {
//				Log.e(LOG_TAG, "Exception during write", e);
//			} catch (InterruptedException e) {
//				Log.e(LOG_TAG, "Exception during write", e);
//			}
//		}
		/**
		 * Write to the connected OutStream.
		 * @param buffer  The data to write
		 */
//		public void write(String buffer) {
//			try {
//				do {
//					Thread.sleep(100);
//				} while ((enabled) && (! ready));
//				if ((enabled) && (ready)){
//					out2.print(buffer);
//					out2.flush();
//				}
//			} catch (InterruptedException e) {
//				Log.e(LOG_TAG, "Exception during write", e);
//			}
//		}
		
		public void close(){
			ready = false;
			try {
	        	Log.d(LOG_TAG, "closing Bluetooth GPS output sream");
				in.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, "error while closing GPS NMEA output stream", e);
			} finally {
//				try {
//		        	Log.d(LOG_TAG, "closing Bluetooth GPS input streams");
//					out2.close();
//					out.close();
//				} catch (IOException e) {
//					Log.e(LOG_TAG, "error while closing GPS input streams", e);
//				} finally {
//					try {
//			        	Log.d(LOG_TAG, "closing Bluetooth GPS socket");
//						socket.close();
//					} catch (IOException e) {
//						Log.e(LOG_TAG, "error while closing GPS socket", e);
//					}
//				}
			}
		}
	}

	private Service callingService;
//	private BluetoothSocket gpsSocket;
	private File gpsDev ;
	private String gpsDeviceAddress;
	private NmeaParser parser = new NmeaParser(10f);
	private boolean enabled = false;
	private ExecutorService notificationPool;
	private ScheduledExecutorService connectionAndReadingPool;
	private List<NmeaListener> nmeaListeners = Collections.synchronizedList(new LinkedList<NmeaListener>()); 
	private LocationManager locationManager;
	private SharedPreferences sharedPreferences;
	private ConnectedGps connectedGps;
	private int disableReason = 0;
	private Notification connectionProblemNotification;
	private Notification serviceStoppedNotification;
	private Context appContext;
	private NotificationManager notificationManager;
	private int maxConnectionRetries;
	private int nbRetriesRemaining;
	private boolean connected = false;
	private boolean setDeviceSpeed = false;
	private String deviceSpeed = "auto";

	/**
	 * @param callingService
	 * @param deviceAddress
	 * @param maxRetries
	 */
	public BlueetoothGpsManager(Service callingService, String deviceAddress, int maxRetries) {
		this.gpsDeviceAddress = deviceAddress;
		this.callingService = callingService;
		this.maxConnectionRetries = maxRetries;
		this.nbRetriesRemaining = 1+maxRetries;
		this.appContext = callingService.getApplicationContext();
		locationManager = (LocationManager)callingService.getSystemService(Context.LOCATION_SERVICE);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(callingService);
		deviceSpeed = sharedPreferences.getString(BluetoothGpsProviderService.PREF_GPS_DEVICE_SPEED, callingService.getString(R.string.defaultGpsDeviceSpeed));
		setDeviceSpeed = !deviceSpeed.equals(callingService.getString(R.string.defaultGpsDeviceSpeed));
		notificationManager = (NotificationManager)callingService.getSystemService(Context.NOTIFICATION_SERVICE);
		parser.setLocationManager(locationManager);	
		
		connectionProblemNotification = new Notification();
		connectionProblemNotification.icon = R.drawable.ic_stat_notify;
		Intent stopIntent = new Intent(BluetoothGpsProviderService.ACTION_STOP_GPS_PROVIDER);
		// PendingIntent stopPendingIntent = PendingIntent.getService(appContext, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		PendingIntent stopPendingIntent = PendingIntent.getService(appContext, 0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		connectionProblemNotification.contentIntent = stopPendingIntent;

		serviceStoppedNotification = new Notification();
		serviceStoppedNotification.icon=R.drawable.ic_stat_notify;
		Intent restartIntent = new Intent(BluetoothGpsProviderService.ACTION_START_GPS_PROVIDER);
		PendingIntent restartPendingIntent = PendingIntent.getService(appContext, 0, restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		serviceStoppedNotification.setLatestEventInfo(appContext, 
				appContext.getString(R.string.service_closed_because_connection_problem_notification_title), 
				appContext.getString(R.string.service_closed_because_connection_problem_notification), 
				restartPendingIntent);
	}

	private void setDisableReason(int reasonId){
		disableReason = reasonId;
	}
	
	/**
	 * @return
	 */
	public int getDisableReason(){
		return disableReason;
	}
	
	/**
	 * @return true if the bluetooth GPS is enabled
	 */
	public synchronized boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables the bluetooth GPS Provider.
	 * @return
	 */
	public synchronized boolean enable() {
		notificationManager.cancel(R.string.service_closed_because_connection_problem_notification_title);
		if (! enabled){
        	Log.d(LOG_TAG, "enabling Bluetooth GPS manager");
//			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//	        if (bluetoothAdapter == null) {
//	            // Device does not support Bluetooth
//	        	Log.e(LOG_TAG, "Device does not support Bluetooth");
//	        	disable(R.string.msg_bluetooth_unsupported);
//	        } else if (!bluetoothAdapter.isEnabled()) {
//	        	// Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//	        	// startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//	        	Log.e(LOG_TAG, "Bluetooth is not enabled");
//	        	disable(R.string.msg_bluetooth_disabled);
//	        } else 
	        if (Settings.Secure.getInt(callingService.getContentResolver(),Settings.Secure.ALLOW_MOCK_LOCATION, 0)==0){
	        	Log.e(LOG_TAG, "Mock location provider OFF");
	        	disable(R.string.msg_mock_location_disabled);
//	        } else if ( (! locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
//	        		 && (sharedPreferences.getBoolean(BluetoothGpsProviderService.PREF_REPLACE_STD_GPS, true))
//	        			) {
//	        	Log.e(LOG_TAG, "GPS location provider OFF");
//	        	disable(R.string.msg_gps_provider_disabled);
	        } else {
//				final BluetoothDevice gpsDevice = bluetoothAdapter.getRemoteDevice(gpsDeviceAddress);
				final String gpsDevice = gpsDeviceAddress;
				if (gpsDevice == null){
					Log.e(LOG_TAG, "GPS device not found");       	    	
		        	disable(R.string.msg_bluetooth_gps_unavaible);
				} else {
	    			Log.e(LOG_TAG, "current device: "+gpsDevice );
//					try {
//						gpsSocket = gpsDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
						gpsDev = new File(gpsDeviceAddress);
//					} catch (IOException e) {
//	    				Log.e(LOG_TAG, "Error during connection", e);
//	    				gpsDev = null;
//					}
					if (gpsDev == null){
	    				Log.e(LOG_TAG, "Error while establishing connection: no socket");
			        	disable(R.string.msg_bluetooth_gps_unavaible);
					} else {
						Runnable connectThread = new Runnable() {							
							public void run() {
								try {
									connected = false;
									Log.v(LOG_TAG, "current device: "+gpsDevice);
									if (/*(bluetoothAdapter.isEnabled()) && */(nbRetriesRemaining > 0 )){										
//										try {
											if (connectedGps != null){
												connectedGps.close();
											}
//											if ((gpsDev != null) && ((connectedGps == null) || (connectedGps.gpsDev != gpsDev))){
//												Log.d(LOG_TAG, "trying to close old socket");
//												gpsSocket.close();
//											}
//										} catch (IOException e) {
//											Log.e(LOG_TAG, "Error during disconnection", e);
//										}
//										try {
//											gpsSocket = gpsDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
											gpsDev = new File(gpsDeviceAddress);
											// verify if we have enough rights..
											Log.v(LOG_TAG, "Will verify if device exists and is a file: "+gpsDev.getAbsolutePath());
											if (gpsDev.exists()){		
												Log.v(LOG_TAG, "Device exists and is a file: "+gpsDev.getAbsolutePath());
												if (! gpsDev.canRead()){
													Log.v(LOG_TAG, "Device is not readable, will try chmod 666 "+gpsDev.getAbsolutePath());
													try {
														// Try to get root privileges  
														Process p = Runtime.getRuntime().exec("su");
														// change device rights
														PrintStream os = new PrintStream(p.getOutputStream(),true);
														os.println("chmod 666 "+ gpsDev.getAbsolutePath());
														// exit
														os.println("exit");
														try {
															p.waitFor();  
//															if (p.exitValue() != 255) {  
//															}  
//															else {  
//															}  
														} catch (InterruptedException e) {  
															// TODO update message
															Log.e(LOG_TAG, "Error while establishing connection: ", e);
														} finally {
															p.destroy();
														}
													} catch (IOException e) {
														// TODO update message
														Log.e(LOG_TAG, "Error while establishing connection: ", e);
														gpsDev = null;														
													} catch (SecurityException e) {
														// TODO update message
														Log.e(LOG_TAG, "Error while establishing connection: ", e);
														gpsDev = null;
													}   
												} else {
													Log.v(LOG_TAG, "Device is readable: "+gpsDev.getAbsolutePath());
												}
												if (setDeviceSpeed){
													Log.v(LOG_TAG, "will set devive spped: " + deviceSpeed);
													try {
														// Try to get root privileges  
														Process p = Runtime.getRuntime().exec("su");
														// change device speed
														PrintStream os = new PrintStream(p.getOutputStream(),true);
														os.println("stty -F "+ gpsDev.getAbsolutePath() + " ispeed "+deviceSpeed);
														// exit
														os.println("exit");
														try {
															p.waitFor();  
//															if (p.exitValue() != 255) {  
//															}  
//															else {  
//															}  
														} catch (InterruptedException e) {  
															// TODO update message
															Log.e(LOG_TAG, "Error while changing device speed: ", e);
														} finally {
															p.destroy();
														}
													} catch (IOException e) {
														// TODO update message
														Log.e(LOG_TAG, "Error while while changing device speed: ", e);
														// gpsDev = null;														
													} catch (SecurityException e) {
														// TODO update message
														Log.e(LOG_TAG, "Error while changing device speed: ", e);
														// gpsDev = null;
													}   
												} else {
													Log.v(LOG_TAG, "Device speed: "+deviceSpeed);
												}
											} else {
												Log.e(LOG_TAG, "Device doesn't exist: "+gpsDev.getAbsolutePath());
												gpsDev = null;
											}
//										} catch (IOException e) {
//											Log.e(LOG_TAG, "Error during connection", e);
//						    				gpsDev = null;
//										}
										if (gpsDev == null){
											Log.e(LOG_TAG, "Error while establishing connection: no device");
								        	disable(R.string.msg_bluetooth_gps_unavaible);
										} else {
											// start GPS device
//											try {
//												File gpsControl = new File("/sys/devices/platform/gps_control/enable");
//												if (gpsControl.isFile()){												
//													if (gpsControl.canWrite()){
//														OutputStream os = new FileOutputStream(gpsControl);
//														os.write('1');
//														os.flush();
//														os.close();
//													}
//												}
//											} catch (FileNotFoundException e) {
//												// TODO update message
//												Log.e(LOG_TAG, "Error while starting GPS: ", e);
//											} catch (IOException e) {
//												// TODO update message
//												Log.e(LOG_TAG, "Error while starting GPS: ", e);
//											} catch (SecurityException e) {
//												// TODO update message
//												Log.e(LOG_TAG, "Error while starting GPS: ", e);
//											}
											// Cancel discovery because it will slow down the connection
//											bluetoothAdapter.cancelDiscovery();
											// we increment the number of connection tries
											// Connect the device through the socket. This will block
											// until it succeeds or throws an exception
											Log.v(LOG_TAG, "connecting to socket");
//											gpsDev.connect();
						        			Log.d(LOG_TAG, "connected to socket");
											connected = true;
											// reset eventual disabling cause
//											setDisableReason(0);
											// connection obtained so reset the number of connection try
											nbRetriesRemaining = 1+maxConnectionRetries ;
											notificationManager.cancel(R.string.connection_problem_notification_title);
						        			Log.v(LOG_TAG, "starting socket reading task");
											connectedGps = new ConnectedGps(gpsDev);
											connectionAndReadingPool.execute(connectedGps);
								        	Log.v(LOG_TAG, "socket reading thread started");
										}
//									} else if (! bluetoothAdapter.isEnabled()) {
//										setDisableReason(R.string.msg_bluetooth_disabled);
									}
//								} catch (IOException connectException) {
//									// Unable to connect
//									Log.e(LOG_TAG, "error while connecting to socket", connectException);									
//									// disable(R.string.msg_bluetooth_gps_unavaible);
								} finally {
									nbRetriesRemaining--;
									if (! connected) {
										disableIfNeeded();
									}
								}
							}
						};
						this.enabled = true;
			        	Log.d(LOG_TAG, "Bluetooth GPS manager enabled");
			        	Log.v(LOG_TAG, "starting notification thread");
						notificationPool = Executors.newSingleThreadExecutor();
			        	Log.v(LOG_TAG, "starting connection and reading thread");
						connectionAndReadingPool = Executors.newSingleThreadScheduledExecutor();
			        	Log.v(LOG_TAG, "starting connection to socket task");
						connectionAndReadingPool.scheduleWithFixedDelay(connectThread, 5000, 60000, TimeUnit.MILLISECONDS);
					}
				}
			}
		}
		return this.enabled;
	}
	
	/**
	 * Disables the bluetooth GPS Provider if the maximal number of connection retries is exceeded.
	 * This is used when there are possibly non fatal connection problems. 
	 * In these cases the provider will try to reconnect with the bluetooth device 
	 * and only after a given retries number will give up and shutdown the service.
	 */
	private synchronized void disableIfNeeded(){
		if (enabled){
			if (nbRetriesRemaining > 0){
				// Unable to connect
				Log.e(LOG_TAG, "Unable to establish connection");
				connectionProblemNotification.when = System.currentTimeMillis();
				String pbMessage = appContext.getResources().getQuantityString(R.plurals.connection_problem_notification, nbRetriesRemaining, nbRetriesRemaining);
				connectionProblemNotification.setLatestEventInfo(appContext, 
						appContext.getString(R.string.connection_problem_notification_title), 
						pbMessage, 
						connectionProblemNotification.contentIntent);
				connectionProblemNotification.number = 1 + maxConnectionRetries - nbRetriesRemaining;
				notificationManager.notify(R.string.connection_problem_notification_title, connectionProblemNotification);
			} else {
//				notificationManager.cancel(R.string.connection_problem_notification_title);
//				serviceStoppedNotification.when = System.currentTimeMillis();
//				notificationManager.notify(R.string.service_closed_because_connection_problem_notification_title, serviceStoppedNotification);
				disable(R.string.msg_two_many_connection_problems);
			}
		}
	}
	
	/**
	 * Disables the bluetooth GPS provider.
	 * 
	 * It will: 
	 * <ul>
	 * 	<li>close the connection with the bluetooth device</li>
	 * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
	 * 	<li>stop the BlueGPS4Droid service</li>
	 * </ul>
	 * The reasonId parameter indicates the reason to close the bluetooth provider. 
	 * If its value is zero, it's a normal shutdown (normally, initiated by the user).
	 * If it's non-zero this value should correspond a valid localized string id (res/values..../...) 
	 * which will be used to display a notification.
	 * 
	 * @param reasonId	the reason to close the bluetooth provider.
	 */
	public synchronized void disable(int reasonId) {
    	Log.d(LOG_TAG, "disabling Bluetooth GPS manager reason: "+callingService.getString(reasonId));
		setDisableReason(reasonId);
    	disable();
	}
		
	/**
	 * Disables the bluetooth GPS provider.
	 * 
	 * It will: 
	 * <ul>
	 * 	<li>close the connection with the bluetooth device</li>
	 * 	<li>disable the Mock Location Provider used for the bluetooth GPS</li>
	 * 	<li>stop the BlueGPS4Droid service</li>
	 * </ul>
	 * If the bluetooth provider is closed because of a problem, a notification is displayed.
	 */
	public synchronized void disable() {
		notificationManager.cancel(R.string.connection_problem_notification_title);
		if (getDisableReason() != 0){
			serviceStoppedNotification.when = System.currentTimeMillis();
			serviceStoppedNotification.setLatestEventInfo(appContext, 
					appContext.getString(R.string.service_closed_because_connection_problem_notification_title), 
					appContext.getString(R.string.service_closed_because_connection_problem_notification, appContext.getString(getDisableReason())),
					serviceStoppedNotification.contentIntent);
			notificationManager.notify(R.string.service_closed_because_connection_problem_notification_title, serviceStoppedNotification);
		}
		if (enabled){
        	Log.d(LOG_TAG, "disabling Bluetooth GPS manager");
			enabled = false;
			connectionAndReadingPool.shutdown();
			// stop GPS device
//			try {
//				File gpsControl = new File("/sys/devices/platform/gps_control/enable");
//				if (gpsControl.isFile()){												
//					if (gpsControl.canWrite()){
//						OutputStream os = new FileOutputStream(gpsControl);
//						os.write('0');
//						os.flush();
//						os.close();
//					}
//				}
//			} catch (FileNotFoundException e) {
//				// TODO update message
//				Log.e(LOG_TAG, "Error while stoping GPS: ", e);
//			} catch (IOException e) {
//				// TODO update message
//				Log.e(LOG_TAG, "Error while stoping GPS: ", e);
//			} catch (SecurityException e) {
//				// TODO update message
//				Log.e(LOG_TAG, "Error while stoping GPS: ", e);
//			}
			Runnable closeAndShutdown = new Runnable() {				
				public void run(){
					try {
						connectionAndReadingPool.awaitTermination(10, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!connectionAndReadingPool.isTerminated()){
						connectionAndReadingPool.shutdownNow();
						if (connectedGps != null){
							connectedGps.close();
						}
//						if ((gpsDev != null) && ((connectedGps == null) || (connectedGps.gpsDev != gpsDev))){
//							try {
//								Log.d(LOG_TAG, "closing Bluetooth GPS socket");
//								gpsDev.close();
//							} catch (IOException closeException) {
//								Log.e(LOG_TAG, "error while closing socket", closeException);
//							}
//						}
					}
				}
			};
			notificationPool.execute(closeAndShutdown);
			nmeaListeners.clear();
			disableMockLocationProvider();
			notificationPool.shutdown();
//			connectionAndReadingPool.shutdown();
			callingService.stopSelf();
        	Log.d(LOG_TAG, "Bluetooth GPS manager disabled");
		}
	}

	/**
	 * Enables the Mock GPS Location Provider used for the bluetooth GPS.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#enableMockLocationProvider(java.lang.String)

	 * @param gpsName	the name of the Location Provider to use for the bluetooth GPS
	 * @param force		true if we want to force auto-activation of the mock location provider (and bypass user preference).
	 */
	public void enableMockLocationProvider(String gpsName, boolean force){
		if (parser != null){
	       	Log.d(LOG_TAG, "enabling mock locations provider: "+gpsName);
			parser.enableMockLocationProvider(gpsName, force);
		}
	}

	/**
	 * Enables the Mock GPS Location Provider used for the bluetooth GPS.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#enableMockLocationProvider(java.lang.String)

	 * @param gpsName	the name of the Location Provider to use for the bluetooth GPS
	 */
	public void enableMockLocationProvider(String gpsName){
		if (parser != null){
	       	Log.d(LOG_TAG, "enabling mock locations provider: "+gpsName);
	    	boolean force = sharedPreferences.getBoolean(BluetoothGpsProviderService.PREF_FORCE_ENABLE_PROVIDER, true);
			parser.enableMockLocationProvider(gpsName, force);
		}
	}

	/**
	 * Disables the current Mock GPS Location Provider used for the bluetooth GPS.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#disableMockLocationProvider()
	 */
	public void disableMockLocationProvider(){
		if (parser != null){
	       	Log.d(LOG_TAG, "disabling mock locations provider");
			parser.disableMockLocationProvider();
		}
	}

	/**
	 * Getter use to know if the Mock GPS Listener used for the bluetooth GPS is enabled or not.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#isMockGpsEnabled()
	 * 
	 * @return 	true if the Mock GPS Listener used for the bluetooth GPS is enabled.
	 */
	public boolean isMockGpsEnabled() {
		boolean mockGpsEnabled = false;
		if (parser != null){
			mockGpsEnabled = parser.isMockGpsEnabled();
		}
		return mockGpsEnabled;
	}
	/**
	 * Getter for the name of the current Mock Location Provider in use.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#getMockLocationProvider()
	 * 
	 * @return the Mock Location Provider name used for the bluetooth GPS
	 */
	public String getMockLocationProvider() {
		String  mockLocationProvider = null;
		if (parser != null){
			mockLocationProvider = parser.getMockLocationProvider();
		}
		return mockLocationProvider;
	}

	/**
	 * Indicates that the bluetooth GPS Provider is out of service.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#setMockLocationProviderOutOfService()
	 */
	private void setMockLocationProviderOutOfService(){
		if (parser != null){
			parser.setMockLocationProviderOutOfService();
		}
	}

	/**
	 * Adds an NMEA listener.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#addNmeaListener(NmeaListener)
	 * @param listener	a {@link NmeaListener} object to register
	 * @return	true if the listener was successfully added
	 */
	public boolean addNmeaListener(NmeaListener listener){
		if (!nmeaListeners.contains(listener)){
	       	Log.d(LOG_TAG, "adding new NMEA listener");
			nmeaListeners.add(listener);
		}
		return true;
	}

	/**
	 * Removes an NMEA listener.
	 * In fact, it delegates to the NMEA parser. 
	 * 
	 * @see NmeaParser#removeNmeaListener(NmeaListener)
	 * @param listener	a {@link NmeaListener} object to remove 
	 */
	public void removeNmeaListener(NmeaListener listener){
       	Log.d(LOG_TAG, "removing NMEA listener");
		nmeaListeners.remove(listener);
	}

	/**
	 * Notifies the reception of a NMEA sentence from the bluetooth GPS to registered NMEA listeners.
	 * 
	 * @param nmeaSentence	the complete NMEA sentence received from the bluetooth GPS (i.e. $....*XY where XY is the checksum)
	 */
	private void notifyNmeaSentence(final String nmeaSentence){
		if (enabled){
	       	Log.v(LOG_TAG, "parsing and notifying NMEA sentence: "+nmeaSentence);
			String sentence = null;
			try {
				sentence = parser.parseNmeaSentence(nmeaSentence);
			} catch (SecurityException e){
		       	Log.e(LOG_TAG, "error while parsing NMEA sentence: "+nmeaSentence, e);
				// a priori Mock Location is disabled
				sentence = null;
				disable(R.string.msg_mock_location_disabled);
			}
			final String recognizedSentence = sentence;
			final long timestamp = System.currentTimeMillis();
			if (recognizedSentence != null){
				Log.v(LOG_TAG, "notifying NMEA sentence: "+recognizedSentence);
				synchronized(nmeaListeners) {
					for(final NmeaListener listener : nmeaListeners){
						notificationPool.execute(new Runnable(){
							public void run() {
								listener.onNmeaReceived(timestamp, recognizedSentence);
							}					 
						});
					}
				}
			}
		}
	}	

	/**
	 * Sends a NMEA sentence to the bluetooth GPS.
	 * 
	 * @param command	the complete NMEA sentence (i.e. $....*XY where XY is the checksum).
	 */
	public void sendPackagedNmeaCommand(final String command){
		Log.e(LOG_TAG, "Disabled .... sending NMEA sentence: "+command);
//		if (isEnabled()){
//			notificationPool.execute( new Runnable() {			
//				@Override
//				public void run() {
//					while ((enabled) && ((!connected) || (connectedGps == null) || (!connectedGps.isReady()))){
//						Log.v(LOG_TAG, "writing thread is not ready");
//						SystemClock.sleep(500);
//					}
//					if (isEnabled() && (connectedGps != null)){
//						connectedGps.write(command);
//						Log.d(LOG_TAG, "sent NMEA sentence: "+command);
//					}
//				}
//			});
//		}
	}

	/**
	 * Sends a SIRF III binary command to the bluetooth GPS.
	 * 
	 * @param commandHexa	an hexadecimal string representing a complete binary command 
	 * (i.e. with the <em>Start Sequence</em>, <em>Payload Length</em>, <em>Payload</em>, <em>Message Checksum</em> and <em>End Sequence</em>).
	 */
	public void sendPackagedSirfCommand(final String commandHexa){
		Log.d(LOG_TAG, "Disabled sending SIRF sentence: "+commandHexa);
//		if (isEnabled()){
//			final byte[] command = SirfUtils.genSirfCommand(commandHexa);
//			notificationPool.execute( new Runnable() {			
//				@Override
//				public void run() {
//					while ((enabled) && ((!connected) || (connectedGps == null) || (!connectedGps.isReady()))){
//						Log.v(LOG_TAG, "writing thread is not ready");
//						SystemClock.sleep(500);
//					}
//					if (isEnabled() && (connectedGps != null)){
//						connectedGps.write(command);
//						Log.d(LOG_TAG, "sent SIRF sentence: "+commandHexa);
//					}
//				}
//			});
//		}
	}

	/**
	 * Sends a NMEA sentence to the bluetooth GPS.
	 * 
	 * @param sentence	the NMEA sentence without the first "$", the last "*" and the checksum.
	 */
	public void sendNmeaCommand(String sentence){
		String command = String.format((Locale)null,"$%s*%X\r\n", sentence, NmeaParser.checkSum(sentence));
		sendPackagedNmeaCommand(command);
	}

	/**
	 * Sends a SIRF III binary command to the bluetooth GPS.
	 * 
	 * @param payload	an hexadecimal string representing the payload of the binary command
	 * (i.e. without <em>Start Sequence</em>, <em>Payload Length</em>, <em>Message Checksum</em> and <em>End Sequence</em>).
	 */
	public void sendSirfCommand(String payload){
		String command = SirfUtils.createSirfCommandFromPayload(payload);
		sendPackagedSirfCommand(command);
	}
}
