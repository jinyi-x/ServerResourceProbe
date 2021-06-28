package org.emoe;
import java.io.File;
import java.util.Date;
import java.net.Socket;
import java.util.Base64;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResProbeClient {
    public static void main(String[] args) throws IOException {
        clientBackend backend = new clientBackend(Integer.parseInt(args[0]), args[1]);
        backend.start();
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
    private static String secondToTime(long second) {
        long days = second / 86400;//转换天数
        second = second % 86400;//剩余秒数
        long hours = second / 3600;//转换小时数
        second = second % 3600;//剩余秒数
        long minutes = second / 60;//转换分钟

        if (0 < days){
            return days + " Days "+hours+" h "+minutes+" min ";
        }else {
            return hours+" h "+minutes+" min ";
        }
    }
    private static class clientBackend extends Thread{
        private static Socket clientSocket;
        private static final byte[] recvByte = new byte[65535];
        private static final ObjectMapper objMapper = new ObjectMapper();
        private static final Base64.Encoder encoder = Base64.getEncoder();
        private static final JacksonObjects.userJsonObject jsonObj = new JacksonObjects.userJsonObject();
        private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        public clientBackend(int remotePort, String remoteAddress) throws IOException{
            clientSocket = new Socket(remoteAddress, remotePort);
            System.out.println("Now Starting Monitoring Server Status");
        }
        public void run(){
            while (!this.isInterrupted()){
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Date currentDate = new Date();
                String tokenStr = dateFormat.format(currentDate);
                tokenStr = calcMD5(tokenStr);
                tokenStr = encoder.encodeToString(tokenStr.getBytes(StandardCharsets.UTF_8));
                tokenStr = tokenStr.substring(0, tokenStr.length() - 1);
                jsonObj.setToken(tokenStr);
                System.out.println(tokenStr);
                try {
                    String sendBuf = objMapper.writeValueAsString(jsonObj);
                    OutputStream clientOutputStream = clientSocket.getOutputStream();
                    clientOutputStream.write(sendBuf.getBytes(StandardCharsets.UTF_8));
                    clientOutputStream.flush();
                    InputStream clientInputStream = clientSocket.getInputStream();
                    int nBytes = clientInputStream.read(recvByte);
                    String recvBuf;
                    if(nBytes > 0)
                        recvBuf = new String(recvByte);
                    else
                        recvBuf = null;
                    JacksonObjects.srvJsonObject srvObj = objMapper.readValue(recvBuf, JacksonObjects.srvJsonObject.class);
                    float cpuUsage = srvObj.getCpuUsage();
                    float memUsage = srvObj.getMemUsage();
                    long srvRunningTime = srvObj.getSrvRunningTime();
                    String runningTime = secondToTime(srvRunningTime / 1000);
                    System.out.printf("CPU Usage: %.2f%%, Memory Usage: %.2f%%, Server Running Time: %s\n", cpuUsage * 100, memUsage * 100, runningTime);
                    File fJson = new File("./args.json");
                    if(!fJson.exists())
                        fJson.createNewFile();
                    FileChannel fChannel;
                    RandomAccessFile randomAccessFile = new RandomAccessFile("./args.lck", "rw");
                    fChannel = randomAccessFile.getChannel();
                    FileLock fLock = null;
                    JacksonObjects.argsJsonObject argsObj = new JacksonObjects.argsJsonObject();
                    argsObj.setCpuUsage(String.format("CPU Usage: %.2f%%", cpuUsage * 100));
                    argsObj.setMemUsage(String.format("Memory Usage: %.2f%%", memUsage * 100));
                    argsObj.setRunningTime("Alive For:" + runningTime);
                    String argsStr = objMapper.writeValueAsString(argsObj);
                    while (fLock == null){
                        try{
                            fLock = fChannel.lock();
                            FileWriter fWriter = new FileWriter(fJson);
                            fWriter.write(argsStr);
                            fWriter.flush();
                            fWriter.close();
                            fLock.release();
                        } catch (IOException e){
                            System.out.println("Can not get lock ./args.lck, Retrying.....");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
