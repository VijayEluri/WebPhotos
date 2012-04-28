/**
 * Copyright 2008 WebPhotos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.webphotos.dao.jpa;

import net.sf.webphotos.model.PhotoVO;

/**
 *
 * @author Guilherme
 */
public class PhotoDAO extends WebPhotosDAO<PhotoVO, Integer> {

    public PhotoDAO() {
        super(PhotoVO.class);
    }
    
}