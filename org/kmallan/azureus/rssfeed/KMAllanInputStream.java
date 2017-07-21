/*
 * RSSFeed - Azureus2 Plugin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package org.kmallan.azureus.rssfeed;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class KMAllanInputStream extends ObjectInputStream {

  private static final byte BC_VECTOR = (byte)0x80, BC_VECTOR_VRSN = (byte)0x80, BC_VECTOR_ITM = (byte)0x81;
  private static final byte BC_URL_BEAN = (byte)0x82, BC_FILTER_BEAN = (byte)0x83, BC_HIST_BEAN = (byte)0x84;
  private static final byte BC_TYPE_STRING = (byte)0x90, BC_TYPE_INT = (byte)0x91, BC_TYPE_LONG = (byte)0x92;
  private static final byte BC_TYPE_BOOLEAN = (byte)0x93, BC_TYPE_UNKNOWN = (byte)0x9F;

  private static final String[] strings = new String[] {"Name", "StoreDir", "Expression", "Type", "Category", "Mode",
                                                        "Location", "Referer", "Cookie", "FileData"};

  private static final String[] bools = new String[] {"IsRegex", "MatchTitle", "MatchLink", "MoveTop", "RateUseCustom",
                                                      "RenameFile", "RenameIncEpisode", "DisableAfter", "CleanFile",
                                                      "Enabled", "ObeyTTL", "LocRef", "UseCookie"};

  private static String[] ints = new String[] {"State", "Priority", "RateUpload", "RateDownload", "StartSeason",
                                               "StartEpisode", "EndSeason", "EndEpisode", "Delay", "SeasonStart",
                                               "SeasonEnd", "EpisodeStart", "EpisodeEnd"};

  private static String[] longs = new String[] {"ID", "Feed", "FiltID"};

  private static Map types;
  static {
    types = new HashMap();
    for(int i = 0; i < strings.length; i++) types.put(strings[i], new Byte(BC_TYPE_STRING));
    for(int i = 0; i < bools.length; i++) types.put(bools[i], new Byte(BC_TYPE_BOOLEAN));
    for(int i = 0; i < ints.length; i++) types.put(ints[i], new Byte(BC_TYPE_INT));
    for(int i = 0; i < longs.length; i++) types.put(longs[i], new Byte(BC_TYPE_LONG));
  }

  private static byte getType(String name) {
    if(types.containsKey(name)) return ((Byte)types.get(name)).byteValue();
    else return BC_TYPE_UNKNOWN;
  }

  public KMAllanInputStream(InputStream in) throws IOException {
    super(in);
  }

  public Vector readVector(String mode) throws IOException, ClassNotFoundException {
    if(readByte() != BC_VECTOR) throw new StreamCorruptedException();

    Vector vect = new Vector();
    if(mode.equalsIgnoreCase("UrlBean")) {
      return readBean(UrlBean.class, BC_URL_BEAN, vect);
    } else if(mode.equalsIgnoreCase("FilterBean")) {
      return readBean(FilterBean.class, BC_FILTER_BEAN, vect);
    } else if(mode.equalsIgnoreCase("HistBean")) {
      return readBean(HistoryBean.class, BC_HIST_BEAN, vect);
    } else {
      throw new StreamCorruptedException("Bad Mode: " + mode);
    }
  }

  private Vector readBean(Class beanClass, byte type, Vector vect) throws IOException, ClassNotFoundException {
    int vectCount = readInt();
    int vectVRSN = 0;
    byte vb = readByte();
    if(vb == BC_VECTOR_VRSN) {
      vectVRSN = readInt();
      vb = readByte();
    }
    if(vb != BC_VECTOR_ITM) throw new StreamCorruptedException();
    if(vectVRSN <= 0) throw new StreamCorruptedException("Unsuported Options File Version: " + vectVRSN);

    int itemCount = readInt();
    String[] items = new String[itemCount];
    for(int i = 0; i < itemCount; i++) items[i] = (String)readObject();

    for(int n = 0; n < vectCount; n++) {
      if(readByte() != type) throw new StreamCorruptedException();

      Object bean = null;
      try {
        bean = beanClass.newInstance();
      } catch(Exception e) {
        throw new IOException("Unable to create bean instance for '" + beanClass.getName() + "' - " + e);
      }

      for(int i = 0; i < itemCount; i++) {
        byte itemType = getType(items[i]);
        byte itemStoreType;
        if(vectVRSN == 1) itemStoreType = getStringType();
        else itemStoreType = readByte();

        if(itemStoreType == BC_TYPE_UNKNOWN) continue;

        if(itemStoreType == itemType) {
          switch(itemStoreType) {
            case BC_TYPE_STRING:
              setValue(bean, items[i], (String)readObject(), String.class);
              break;
            case BC_TYPE_INT:
              setValue(bean, items[i], new Integer(readInt()), int.class);
              break;
            case BC_TYPE_LONG:
              setValue(bean, items[i], new Long(readLong()), long.class);
              break;
            case BC_TYPE_BOOLEAN:
              setValue(bean, items[i], new Boolean(readBoolean()), boolean.class);
              break;
          }
        } else {
          switch(itemStoreType) {
            case BC_TYPE_STRING:
              readObject();
              break;
            case BC_TYPE_INT:
              readInt();
              break;
            case BC_TYPE_LONG:
              readLong();
              break;
            case BC_TYPE_BOOLEAN:
              readBoolean();
              break;
            default: throw new StreamCorruptedException();
          }
        }
      }
      vect.add(bean);
    }
    return vect;
  }

  private byte getStringType() throws IOException, ClassNotFoundException {
    byte byteType = BC_TYPE_UNKNOWN;
    String stringType = (String)readObject();
    if(stringType.equalsIgnoreCase("String")) byteType = BC_TYPE_STRING;
    else if(stringType.equalsIgnoreCase("Int")) byteType = BC_TYPE_INT;
    else if(stringType.equalsIgnoreCase("Long")) byteType = BC_TYPE_LONG;
    else if(stringType.equalsIgnoreCase("Boolean")) byteType = BC_TYPE_BOOLEAN;
    else throw new StreamCorruptedException("Bad Item Type: " + stringType);
    return byteType;
  }

  private void setValue(Object target, String name, Object value, Class type) throws StreamCorruptedException {
    try {
      Method m = target.getClass().getMethod("set" + name, new Class[] {type});
      m.invoke(target, new Object[] {value});
    } catch(Exception e) {
      throw new StreamCorruptedException(e.toString());
    }
  }

}
