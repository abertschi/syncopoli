package org.amoradi.syncopoli;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class BackupItem implements Parcelable {
    public enum Direction {
        INCOMING,
        OUTGOING
    };

    public String name;
    public String source;
    public String destination;
    public String logFileName;
    public Date lastUpdate;
    public Direction direction;

    public String rsync_options;

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeString(source);
		out.writeString(destination);
		out.writeString(logFileName);

		Format ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			out.writeString(ft.format(lastUpdate));
		} catch (IllegalArgumentException e) {
			out.writeString(ft.format(new Date()));
		}

		if (direction == Direction.OUTGOING) {
			out.writeString("OUTGOING");
		} else {
			out.writeString("INCOMING");
		}

		out.writeString(rsync_options);
	}
	
	public static final Parcelable.Creator<BackupItem> CREATOR
		= new Parcelable.Creator<BackupItem>()
	{
		public BackupItem createFromParcel(Parcel in) {
			BackupItem b = new BackupItem();
			b.name = in.readString();
			b.source = in.readString();
			b.destination = in.readString();
			b.logFileName = in.readString();

			SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String x = in.readString();
			try {
				b.lastUpdate = ft.parse(x);
			} catch (ParseException e) {
				Log.e("BackupItem", "Could not parse date string from parcelable: " + x);
				b.lastUpdate = new Date();
			}

			String y = in.readString();
			if (y.equals("OUTGOING")) {
				b.direction = Direction.OUTGOING;
			} else {
				b.direction = Direction.INCOMING;
			}

			b.rsync_options = in.readString();

			return b;
		}

		public BackupItem[] newArray(int size) {
			return new BackupItem[size];
		}
	};

}
