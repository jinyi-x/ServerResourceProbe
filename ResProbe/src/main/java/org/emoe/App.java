package org.emoe;
import oshi.util.Util;
import java.util.Date;
import java.net.Socket;
import oshi.SystemInfo;
import java.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import oshi.hardware.GlobalMemory;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import oshi.hardware.CentralProcessor;
import java.nio.charset.StandardCharsets;
import java.lang.management.ManagementFactory;
import java.security.NoSuchAlgorithmException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    public static void main( String[] args ) throws IOException {
        Thread srvBackendThread = new srvBackend(12864);
        srvBackendThread.start();
    }
    private static String calcMD5(String input) {
        if(input == null || input.length() == 0) {
            return null;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(input.getBytes());
            byte[] byteArray = md5.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : byteArray) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static boolean verifyToken(String token){
        boolean isValid;
        String processingStr = token + "=";
        final Base64.Decoder b64Dec = Base64.getDecoder();
        System.out.println(processingStr);
        processingStr = new String(b64Dec.decode(processingStr));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date currentDate = new Date();
        String srvVerifyString = dateFormat.format(currentDate);
        srvVerifyString = calcMD5(srvVerifyString);
        isValid = processingStr.equals(srvVerifyString);
        return isValid;
    }
    private static class srvBackend extends Thread {
        private final ServerSocket srvSocket;
        public srvBackend(int Port) throws IOException{
            srvSocket = new ServerSocket(Port);
        }
        @Override
        public void run() {
            super.run();
            boolean isClosed = true;
            Socket client = null;

            while (!this.isInterrupted()){
                try {
                    if(isClosed){
                        client = srvSocket.accept();
                        isClosed = false;
                    }
                    InputStream clientInputStream = client.getInputStream();
                    byte[] clientByteArr = new byte[65535];
                    clientInputStream.read(clientByteArr);
                    String jsonStr = new String(clientByteArr);
                    ObjectMapper objMapper = new ObjectMapper();
                    JacksonObjects.userJsonObject jsonObj = objMapper.readValue(jsonStr, JacksonObjects.userJsonObject.class);
                    JacksonObjects.srvJsonObject srvObj = new JacksonObjects.srvJsonObject();
                    if(verifyToken(jsonObj.getToken())){
                        srvObj.setState(true);
                        srvObj.setSrvRunningTime(ManagementFactory.getRuntimeMXBean().getUptime());
                        SystemInfo sysInfo = new SystemInfo();
                        GlobalMemory sysMem = sysInfo.getHardware().getMemory();
                        long totalMem = sysMem.getTotal();
                        long availableMem = sysMem.getAvailable();
                        srvObj.setMemUsage(1 - ((float)availableMem / (float)totalMem));
                        CentralProcessor cpu = sysInfo.getHardware().getProcessor();
                        long[] prevTicks = cpu.getSystemCpuLoadTicks();
                        Util.sleep(1000);
                        long[] ticks = cpu.getSystemCpuLoadTicks();
                        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
                        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
                        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
                        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
                        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
                        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
                        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
                        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
                        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
                        srvObj.setCpuUsage(1 - ((float)idle / (float) totalCpu));
                    } else {
                        srvObj.setState(false);
                        srvObj.setCpuUsage(0.00F);
                        srvObj.setMemUsage(0.00F);
                        srvObj.setSrvRunningTime(0L);
                    }
                    String srvStr = objMapper.writeValueAsString(srvObj);
                    OutputStream clientOutputStream = client.getOutputStream();
                    clientOutputStream.write(srvStr.getBytes(StandardCharsets.UTF_8));
                    clientOutputStream.flush();
                } catch (IOException e){
                    isClosed = true;
                    e.printStackTrace();
                }
            }
        }
    }
}
