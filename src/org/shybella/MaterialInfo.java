/*
    TekkitCustomizer Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.shybella;

//represents a material or collection of materials
public class MaterialInfo
{
	int typeID;
	int data;
	boolean allDataValues;
	String description;
	String reason;
	
	public MaterialInfo(int typeID, int data, String description, String reason)
	{
		this.typeID = typeID;
		this.data = data;
		this.allDataValues = false;
		this.description = description;
		this.reason = reason;
	}
	
	public MaterialInfo(int typeID, String description, String reason)
	{
		this.typeID = typeID;
		this.data = 0;
		this.allDataValues = true;
		this.description = description;
		this.reason = reason;
	}
	
	private MaterialInfo(int typeID, int data, boolean allDataValues, String description, String reason)
	{
		this.typeID = typeID;
		this.data = data;
		this.allDataValues = allDataValues;
		this.description = description;
		this.reason = reason;
	}
	
	@Override
	public String toString()
	{
		String returnValue = String.valueOf(this.typeID) + ":" + (this.allDataValues?"*":String.valueOf(this.data));
		if(this.description != null) returnValue += ":" + this.description + ":" + this.reason;
		
		return returnValue;
	}
	
	public static MaterialInfo fromString(String string)
	{
		if(string == null || string.isEmpty()) return null;
		
		String [] parts = string.split(":");
		if(parts.length < 2) return null;
		
		try
		{
			int typeID = Integer.parseInt(parts[0]);
		
			int data;
			boolean allDataValues;
			if(parts[1].equals("*"))
			{
				allDataValues = true;
				data = 0;
			}
			else
			{
				allDataValues = false;
				data = Integer.parseInt(parts[1]);
			}
			
			return new MaterialInfo(typeID, data, allDataValues, 
									parts.length >= 3 ? parts[2] : "",
									parts.length >= 4 ? parts[3] : "(No reason provided. Contact staff.)");
		}
		catch(NumberFormatException exception)
		{
			return null;
		}
	}
}