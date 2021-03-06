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
package net.sf.webphotos.gui.util;

import javax.swing.table.*;
import net.sf.webphotos.Album;
import net.sf.webphotos.PhotoDTO;
import net.sf.webphotos.gui.PainelWebFotos;

/**
 * Gera o modelo da tabela de fotos.
 */
public class TableModelFoto extends AbstractTableModel {

    private static final long serialVersionUID = -3797898104363613961L;
    private static final TableModelFoto instancia = new TableModelFoto();
    private Object[][] fotoTabela;
    private String[] fotoColunas;

    private TableModelFoto() {
    }

    /**
     * Retorna a inst�ncia da pr�pria classe.
     * @return Retorna um TableModelFoto.
     */
    public static TableModelFoto getModel() {
        return instancia;
    }

    /**
     * Armazena os dados de fotos em duas vari�veis da classe.
     * Na vari�vel fotoTabela, as fotos e seus dados espec�ficos.
     * E na vari�vel fotoColunas somente os dados espec�ficos.
     */
    public void update() {
        fotoTabela = Album.getAlbum().getFotosArray();
        fotoColunas = Album.getAlbum().getFotosColunas();
    }

    /**
     * Recebe um n�mero referente a uma coluna e retorna o valor da coluna atrav�s do vetor fotoColunas.
     * @param column N�mero referente a coluna.
     * @return Retorna o valor contido na coluna.
     */
    @Override
    public String getColumnName(int column) {
        return fotoColunas[column];
    }

    /**
     * Retorna o total de colunas, contando o n�mero de posi��es no vetor fotoColunas.
     * @return Retorna o total de colunas.
     */
    @Override
    public int getColumnCount() {
        if (fotoColunas == null) {
            return 0;
        }
        return fotoColunas.length;
    }

    /**
     * Retorna o total de linhas, contando o n�mero de posi��es no vetor fotoTabela.
     * @return Retorna o total de linhas.
     */
    @Override
    public int getRowCount() {
        if (fotoTabela == null) {
            return 0;
        }
        return fotoTabela.length;
    }

    /**
     * Busca um valor contido na matriz fotoTabela e retorna um Object.
     * Recebe como par�metro um �ndice de linha e um de coluna para efetuar a procura.
     * @param line N�mero da linha.
     * @param column N�mero da coluna.
     * @return Retorna o valor encontrado em um Object.
     */
    @Override
    public Object getValueAt(int line, int column) {
        return fotoTabela[line][column];
    }

    /**
     * Recebe um valor e os �ndices da matriz e seta esse valor na matriz fotoTabela.
     * Checa se a foto possui ID ou nome, depois testa se o valor � de legenda ou cr�dito e implanta na matriz fotoTabela.
     * @param value Valor a ser implantado.
     * @param line N�mero da linha.
     * @param column N�mero da coluna.
     */
    @Override
    public void setValueAt(Object value, int line, int column) {
        // testar para verificar se fotoID � um n�mero ou um nome de arquivo
        int fotoID = 0;
        String nomeFoto = "";
        try {
            fotoID = Integer.parseInt(fotoTabela[line][0].toString());
        } catch (Exception e) {
            nomeFoto = fotoTabela[line][0].toString();
        }

        // Qual campo est� editando ?
        if (column == 1) {
            // usu�rio est� editando coluna legenda
            // atualiza o modelo
            fotoTabela[line][column] = value;
            // atualiza objeto foto
            if (fotoID > 0) {
                Album.getAlbum().getFoto(fotoID).setLegenda((String) value);
            } else {
                Album.getAlbum().getFoto(nomeFoto).setLegenda((String) value);
            }
            // ajusta o texto da legenda
            PainelWebFotos.getTxtLegenda().setText((String) value);
        } else if (column == 2) {
            // usu�rio est� editando coluna cr�dito (combobox)
            fotoTabela[line][column] = value;
            if (fotoID > 0) {
                Album.getAlbum().getFoto(fotoID).setCreditoNome((String) value);
            } else {
                Album.getAlbum().getFoto(nomeFoto).setCreditoNome((String) value);
            }
            int indice = PhotoDTO.getLstCreditosIndex((String) value);
            // soma 1 ao indice, pois o primeiro value � espa�o vazio
            //PainelWebFotos.getLstCreditos().setSelectedIndex(indice + 1);
            // FIXME : Retornar comportamento original com linha em branco
            PainelWebFotos.getLstCreditos().setSelectedIndex(indice);
        }
    }

    /**
     * Checa se o n�mero de colunas � maior que zero e retorna <I>true</I>, caso contr�rio retorna <I>false</I>.
     * TODO: avaliar a funcionalidade desse m�todo.
     * @param line N�mero da linha.
     * @param column N�mero da coluna.
     * @return Retorna um valor l�gico.
     */
    @Override
    public boolean isCellEditable(int line, int column) {
        if (column > 0) {
            return true;
        }
        return false;
    }

    /**
     * Retorna a classe do objeto encontrado na matriz fotoTabela.
     * Busca a partir do valor do n�mero da coluna recebido como par�metro.
     * @param column N�mero da coluna.
     * @return Retorna uma classe.
     */
    @Override
    public Class<? extends Object> getColumnClass(int column) {
        return fotoTabela[0][column].getClass();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton Object");
    }
}
