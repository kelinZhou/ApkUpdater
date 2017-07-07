package com.chengshi.kelin.updatemanagerdemo;

import android.support.annotation.NonNull;

import com.chengshi.apkUpdater.UpdateInfo;

import java.util.List;

/**
 * 描述 ${TODO}
 * 创建人 kelin
 * 创建时间 2017/3/14  下午2:08
 * 版本 v 1.0.0
 */

public class UpdateModel implements UpdateInfo {

    private String msg;
    private String code;
    private DataBean data;



    @Override
    public int getVersionCode() {
        return data.latest_package.build_code;
    }

    @Override
    public String getDownLoadsUrl() {
        return data.latest_package.download_url;
    }

    @Override
    public boolean isForceUpdate() {
        return data.latest_package.update_level == 1;
    }

    @NonNull
    @Override
    public String getApkName() {
        return data.latest_package.filename;
    }

    @Override
    public CharSequence getUpdateMessage() {
        return String.format("%s%n%n%s", data.latest_package.update_content, "是否现在更新？");
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class DataBean {
        private ProjectBean project;
        private LatestPackageBean latest_package;

        public ProjectBean getProject() {
            return project;
        }

        public void setProject(ProjectBean project) {
            this.project = project;
        }

        public LatestPackageBean getLatest_package() {
            return latest_package;
        }

        public void setLatest_package(LatestPackageBean latest_package) {
            this.latest_package = latest_package;
        }

        public static class ProjectBean {

            private int platform;
            private String uid;
            private String platform_display;
            private String msg_url;
            private String logo;
            private String auto_publish_display;
            private int auto_publish;
            private int id;
            private String download_url;
            private String name;

            public int getPlatform() {
                return platform;
            }

            public void setPlatform(int platform) {
                this.platform = platform;
            }

            public String getUid() {
                return uid;
            }

            public void setUid(String uid) {
                this.uid = uid;
            }

            public String getPlatform_display() {
                return platform_display;
            }

            public void setPlatform_display(String platform_display) {
                this.platform_display = platform_display;
            }

            public String getMsg_url() {
                return msg_url;
            }

            public void setMsg_url(String msg_url) {
                this.msg_url = msg_url;
            }

            public String getLogo() {
                return logo;
            }

            public void setLogo(String logo) {
                this.logo = logo;
            }

            public String getAuto_publish_display() {
                return auto_publish_display;
            }

            public void setAuto_publish_display(String auto_publish_display) {
                this.auto_publish_display = auto_publish_display;
            }

            public int getAuto_publish() {
                return auto_publish;
            }

            public void setAuto_publish(int auto_publish) {
                this.auto_publish = auto_publish;
            }

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getDownload_url() {
                return download_url;
            }

            public void setDownload_url(String download_url) {
                this.download_url = download_url;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        public static class LatestPackageBean {
            /**
             * public_status : 1
             * update_level : 1
             * public_status_display : 已发布
             * filesize : 3627448
             * dependents : []
             * build_code : 120
             * filename : iot_android_1.2.0.20170104.apk
             * update_level_display : 强制更新
             * create_time : 2016-12-27 10:40
             * download_url : http://update.useonline.cn/api/package/file/6e22f738-4fbc-4bd2-8569-b68f5ea7c03e
             * fid : 6e22f738-4fbc-4bd2-8569-b68f5ea7c03e
             * version_name : 1.2.0
             * id : 4
             * update_content : 1.修复了出入库页面在一个执行完成后不能自动进入下一个任务的执行的bug。 2.出入库使用了MQTT协议。
             */

            private int public_status;
            private int update_level;
            private String public_status_display;
            private int filesize;
            private int build_code;
            private String filename;
            private String update_level_display;
            private String create_time;
            private String download_url;
            private String fid;
            private String version_name;
            private int id;
            private String update_content;
            private List<?> dependents;

            public int getPublic_status() {
                return public_status;
            }

            public void setPublic_status(int public_status) {
                this.public_status = public_status;
            }

            public int getUpdate_level() {
                return update_level;
            }

            public void setUpdate_level(int update_level) {
                this.update_level = update_level;
            }

            public String getPublic_status_display() {
                return public_status_display;
            }

            public void setPublic_status_display(String public_status_display) {
                this.public_status_display = public_status_display;
            }

            public int getFilesize() {
                return filesize;
            }

            public void setFilesize(int filesize) {
                this.filesize = filesize;
            }

            public int getBuild_code() {
                return build_code;
            }

            public void setBuild_code(int build_code) {
                this.build_code = build_code;
            }

            public String getFilename() {
                return filename;
            }

            public void setFilename(String filename) {
                this.filename = filename;
            }

            public String getUpdate_level_display() {
                return update_level_display;
            }

            public void setUpdate_level_display(String update_level_display) {
                this.update_level_display = update_level_display;
            }

            public String getCreate_time() {
                return create_time;
            }

            public void setCreate_time(String create_time) {
                this.create_time = create_time;
            }

            public String getDownload_url() {
                return download_url;
            }

            public void setDownload_url(String download_url) {
                this.download_url = download_url;
            }

            public String getFid() {
                return fid;
            }

            public void setFid(String fid) {
                this.fid = fid;
            }

            public String getVersion_name() {
                return version_name;
            }

            public void setVersion_name(String version_name) {
                this.version_name = version_name;
            }

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getUpdate_content() {
                return update_content;
            }

            public void setUpdate_content(String update_content) {
                this.update_content = update_content;
            }

            public List<?> getDependents() {
                return dependents;
            }

            public void setDependents(List<?> dependents) {
                this.dependents = dependents;
            }
        }
    }
}
