/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package javax.servlet.sip;

/**
 * This interface allows applications to set the authentication 
 * information on servlet initiated requests that are challenged by a Proxy or UAS.
 */
public interface AuthInfo {
	
	/**
	 * Helper method to add authentication info into the AuthInfo object 
	 * for a challenge response of a specific type (401/407) and realm.
	 * @param statusCode Status code (401/407) of the challenge response
	 * @param realm Realm that was returned in the challenge response
	 * @param username
	 * @param password
	 */
	void addAuthInfo(int statusCode,
            java.lang.String realm,
            java.lang.String username,
            java.lang.String password);
}
