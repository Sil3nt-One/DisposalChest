package com.sil3ntone.disposalchest;

/**
 * Created by Pratham on 3/29/2017.
 */
public class TrashChestOwner {
    private String uuid;
    private int count;

    public TrashChestOwner(String uuid, int count) {
        this.uuid = uuid;
        this.count = count;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void addCount() {
        this.count++;
    }

    public void subtractCount() {
        if(this.count > 0) {
            this.count--;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrashChestOwner that = (TrashChestOwner) o;

        return getUuid() != null ? getUuid().equals(that.getUuid()) : that.getUuid() == null;
    }

    @Override
    public int hashCode() {
        return getUuid() != null ? getUuid().hashCode() : 0;
    }
}
