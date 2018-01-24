package com.crinity.cmbeat.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.crinity.cmbeat.monitor.Monitor;

/*
 * 프로세스 Thread 상태를 보여주는 클래스
 */
public class ProcessThreadMonitor implements Monitor {
    private BufferedWriter out = null;
    private String filename = null;

    // 일반적인 시작, CSV 파일 생성
    public ProcessThreadMonitor(String filename) {

        this.filename = "./log/processthread/" + filename + ".csv";
        prepareCSV();
    }

    // linux ps 명령어를 이용해 process에서 돌아가고 있는 thread를 받아옴
    private ArrayList<ProcessThreadDao> getProcessThreadInfo()
            throws IOException {
        int pastPid = -1;
        int pid = 0;
        int count = 0;
        float ram = 0.0f;
        String user = null;
        String comm = null;

        String line;
        ArrayList<ProcessThreadDao> processThreadList = new ArrayList<ProcessThreadDao>();

        Process p = Runtime.getRuntime().exec(
                "ps -e -T -o pid,uname,pcpu,pmem,command");
        Scanner input = new Scanner(p.getInputStream());

        input.nextLine(); // ps 명령어 초반 PID USER %CPU %MEM COMMAND 부분 무시
        while (input.hasNext()) {
            ProcessThreadDao pTDao = new ProcessThreadDao();
            String command = "";

            line = input.nextLine();
            StringTokenizer stringTokenizer = new StringTokenizer(line);

            pid = Integer.parseInt(stringTokenizer.nextToken());
            pTDao.setPid(pid);

            // pid가 같으면 cpu를 제외한 user, ram점유율, command가 같다.
            if (pid == pastPid) {
                pTDao.setUser(user);
                stringTokenizer.nextToken();
                pTDao.setCpuUsage(Float.parseFloat(stringTokenizer.nextToken()));
                pTDao.setRamUsage(ram);
                pTDao.setCommand(comm);
                pTDao.setThreadNum(count);
            } else {
                count = 0;

                user = stringTokenizer.nextToken();
                pTDao.setCpuUsage(Float.parseFloat(stringTokenizer.nextToken()));
                ram = Float.parseFloat(stringTokenizer.nextToken());

                // countTokens가 1보다 많도록 한 이유 : 1로 하지 않으면 마지막에 공백이 남기 때문에 while문
                // 공백을 처리하기 위해 token을 하나 남겨두고 while문을 통과한 후에 밑에다가 nextToken 하나
                // 붙여줌
                while (stringTokenizer.countTokens() > 1) {
                    command += stringTokenizer.nextToken() + " ";
                }
                command += stringTokenizer.nextToken();// 인자 없는 command와 마지막 인자
                comm = command;

                pTDao.setUser(user);
                pTDao.setRamUsage(ram);
                pTDao.setCommand(comm);
                pTDao.setThreadNum(count);

                pastPid = pid;
            }
            count += 1;

            processThreadList.add(pTDao);
        }
        input.close();

        return processThreadList;
    }

    @Override
    public void show() {
        ArrayList<ProcessThreadDao> processThreadList = null;
        try {
            processThreadList = getProcessThreadInfo();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("----------------Process------------------");
        for (int i = 0; i < processThreadList.size(); i++) {
            ProcessThreadDao processThread = processThreadList.get(i);

            System.out
                    .println(String
                            .format("PID: %d    User: %s    CPU: %f    RAM: %f    Command: %s   ThreadNum: %d",
                                    processThread.getPid(),
                                    processThread.getUser(),
                                    processThread.getCpuUsage(),
                                    processThread.getRamUsage(),
                                    processThread.getCommand(),
                                    processThread.getThreadNum()));
        }
        System.out.println("------------------------------------------");
    }

    @Override
    public void makeCSV() {
        ArrayList<ProcessThreadDao> processThreadList = null;
        try {
            processThreadList = getProcessThreadInfo();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int i = 0; i < processThreadList.size(); i++) {
            ProcessThreadDao processThread = processThreadList.get(i);

            try {
                out.write(String.format("%d,%s,%f,%f,\"%s\",%d\n",
                        processThread.getPid(), processThread.getUser(),
                        processThread.getCpuUsage(),
                        processThread.getRamUsage(),
                        processThread.getCommand(),
                        processThread.getThreadNum()));
            } catch (IOException e) {
                System.out.println("File IOException!");
                e.printStackTrace();
            }
        }

        try {
            out.flush();
        } catch (IOException e) {
            System.out.println("File IOException!");
            e.printStackTrace();
        }
    }

    @Override
    public void prepareCSV() {
        try {
            File file = new File(filename);
            boolean isExist = file.exists();
            this.out = new BufferedWriter(new FileWriter(file, true));

            if (!isExist) {
                out.write("PID,USER,CPU,MEM,COMM,THREADNUM\n");
            }
        } catch (IOException e) {
            System.out.println("CSV Log File Create or Write Error!");
            e.printStackTrace();
        }
    }
}