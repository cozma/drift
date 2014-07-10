	

    package com.bah.mapping;
     
    public class Hazard {
     
            private String descriptor;
            private String name;
            private String level;
            private String lat1;
            private String lat2;
            private String lng1;
            private String lng2;
            private String mission_ID;
            private String hazard_ID;
           
            public Hazard(String descriptor, String name, String level, String lat1, String lng1, String lat2, String lng2, String mission_ID, String hazard_ID){
                    this.descriptor = descriptor;
                    this.name = name;
                    this.level = level;
                    this.lat1 = lat1;
                    this.lng1 = lng1;
                    this.lat2 = lat2;
                    this.lng2 = lng2;
                    this.mission_ID = mission_ID;
                    this.hazard_ID = hazard_ID;
            }
           
            public String getDescriptor(){
                    return descriptor;
            }
            public void setDescriptor(String descriptor){
                    this.descriptor = descriptor;
            }
            public String getName(){
                    return name;
            }
            public void setName(String name){
                    this.name = name;
            }
            public String getLevel(){
                    return level;
            }
            public void setLevel(String level){
                    this.level = level;
            }
            public double getLat1(){
                    return Double.parseDouble(lat1);
            }
            public void setLat1(String lat1){
                    lat1 = this.lat1;
            }
            public double getLat2(){
                    return Double.parseDouble(lat2);
            }
            public void setLat2(String lat2){
                    lat2 = this.lat2;
            }
            public double getLng1(){
                    return Double.parseDouble(lng1);
            }
            public void setLng1(String lng1){
                    lng1 = this.lng1;
            }
            public double getLng2(){
                    return Double.parseDouble(lng2);
            }
            public void setLng2(String lng2){
                    lng2 = this.lng2;
            }
           
            public String getMissionId(){
                    return mission_ID;
            }
           
            public void setMissionId(String mission_ID){
                    this.mission_ID = mission_ID;
            }
           
            public String getHazardId(){
                    return hazard_ID;
            }
           
            public void setHazardId(String hazard_ID){
                    this.hazard_ID = hazard_ID;
            }
            public String toString(){
                    return descriptor + " " + name + " " + level + ": \n" + lat1 + "," + lng1 + "\n" + lat2 + "," + lng2;
            }
    }

