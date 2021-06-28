package org.emoe;

public class JacksonObjects {
    public static class userJsonObject {
        private String token;
        public String getToken() { return this.token; }
        public void setToken(String token) {this.token = token;}
    }
    public static class srvJsonObject {
        private boolean state;
        private float cpuUsage;
        private float memUsage;
        private long srvRunningTime;
        public boolean getState(){return this.state;}
        public void setState(boolean state) {this.state = state;}
        public float getCpuUsage() {return this.cpuUsage;}
        public void setCpuUsage(float cpuUsage) {this.cpuUsage = cpuUsage;}
        public float getMemUsage() {return this.memUsage;}
        public void setMemUsage(float memUsage) {this.memUsage = memUsage;}
        public long getSrvRunningTime() {return this.srvRunningTime;}
        public void setSrvRunningTime(long srvRunningTime) {this.srvRunningTime = srvRunningTime;}
    }
    public static class argsJsonObject{
        private String cpuUsage;
        private String memUsage;
        private String RunningTime;
        public String getCpuUsage() {return this.cpuUsage;}
        public void setCpuUsage(String cpuUsage) {this.cpuUsage = cpuUsage;}
        public String getMemUsage() {return this.memUsage;}
        public void setMemUsage(String memUsage) {this.memUsage = memUsage;}
        public String getRunningTime() {return this.RunningTime;}
        public void setRunningTime(String runningTime) { this.RunningTime = runningTime; }
    }
}
