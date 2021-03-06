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
package net.sf.webphotos.sync.FTP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import net.sf.webphotos.Album;
import net.sf.webphotos.BancoImagem;
import net.sf.webphotos.PhotoDTO;
import net.sf.webphotos.sync.Sync;
import net.sf.webphotos.sync.SyncEvent;
import net.sf.webphotos.sync.SyncException;
import net.sf.webphotos.sync.SyncListener;
import net.sf.webphotos.util.Util;
import net.sf.webphotos.util.legacy.Arquivo;
import net.sf.webphotos.util.legacy.CacheFTP;
import net.sf.webphotos.util.legacy.ComandoFTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.log4j.Logger;

/**
 * Interface entre o sistema e o envio de arquivos. Herda a classe FTPClient e
 * implementa (sobrescreve) os m�todos da interface Sync. Possui m�todos de
 * transfer�ncia de arquivos, mudan�a de diret�rio, cria��o de subdiret�rio,
 * conex�o e desconex�o ao FTP e carregamento de arquivos de FTP.
 *
 * @author guilherme
 */
public class SyncObject extends FTPClient implements Sync {

    private int reply;
    private ArrayList<Arquivo> listaArquivos = new ArrayList<Arquivo>();
    private String ftpRoot = null;
    private List listFTP;
    private File albunsRoot = Util.getAlbunsRoot();
    private String usuario;
    private char[] senha;
    private long totalBytes = 0;
    private long transmitido = 0;
    private boolean enviarAltaResolucao;
    private SyncListener syncListener = null;
    private CopyStreamListener copyStreamListener = null;
    private String ftpHost;
    private int ftpPort;
    private int retry;
    private static final Logger log = Logger.getLogger(SyncObject.class);

    /**
     * Cria uma nova inst�ncia de FTP
     */
    public SyncObject() {
        super();
        enviarAltaResolucao = Util.getProperty("enviarAltaResolucao").equals("true");
    }

    /**
     * M�todo baseado no storeFile da classe FTPClient, do pacote commons/net.
     * Acrescenta um controle de processamento para saber quanto foi
     * enviado/recebido.
     *
     * @param streamOrigem Arquivo de origem
     * @param streamDestino Local de destino
     * @param streamSize Tamanho do arquivo
     * @throws java.io.IOException Problemas na leitura e escrita dos dados.
     */
    @Override
    public void transferFile(InputStream streamOrigem, OutputStream streamDestino, long streamSize)
            throws IOException {

        org.apache.commons.net.io.Util.copyStream(
                streamOrigem,
                streamDestino,
                getBufferSize(),
                streamSize,
                copyStreamListener,
                true);

        streamDestino.flush();
        streamOrigem.close();
        streamDestino.close();

        completePendingCommand();
    }

    /**
     * Muda de diret�rio. criando o diret�rio quando n�o existir. TODO: Colocar
     * o Util.log para trabalhar no fluxo de execu��o.
     *
     * @param diretorioFilho Diret�rio que deve ser acessado.
     * @throws net.sf.webphotos.sync.SyncException Erro de sincroniza��o.
     * @throws java.io.IOException Erro de comunica��o entre os dados.
     */
    @Override
    public void cd(String diretorioFilho) throws IOException, SyncException {
        changeWorkingDirectory(getSyncFolder());
        Util.out.println(printWorkingDirectory());
        changeWorkingDirectory(diretorioFilho);
        if (getReplyCode() != FTPReply.CODE_250) {
            Util.log(getSyncFolder() + File.separator + diretorioFilho + " n�o existe..criando");
            if (!makeDirectory(diretorioFilho)) {
                throw new SyncException("[FTP.cd]/ERRO: n�o foi poss�vel criar diret�rio " + diretorioFilho + " verifique suas permiss�es com o provedor");
            }
        }
        pwd();
        Util.log("ok cd " + printWorkingDirectory());
    }

    /**
     * Cria um novo subdiret�rio no servidor FTP, no diret�rio atual (se um
     * pathname relativo � dado) ou onde especificado (se um pathname absoluto �
     * dado). Esta � uma vers�o recurssiva que cria os diret�rios somente quando
     * s�o precisos.
     *
     * @param pathName O nome do diret�rio a ser criado.
     * @return True se completou com sucesso, ou false caso n�o.
     * @exception IOException Se um erro de I/O ocorrer enquanto est� enviando
     * comando para o servidor ou recebendo resposta dele.
     */
    @Override
    public boolean makeDirectory(String pathName) throws IOException {
        if (pathName.startsWith("/")) {
            changeWorkingDirectory("/");
            pathName = pathName.substring(1);
        }
        Util.out.println(super.printWorkingDirectory());
        String[] dirs = pathName.split("/");
        for (String dir : dirs) {
            if (!super.printWorkingDirectory().endsWith(dir)) {
                super.changeWorkingDirectory(dir);
                if (!FTPReply.isPositiveCompletion(super.getReplyCode())) {
                    super.makeDirectory(dir);
                    super.changeWorkingDirectory(dir);
                    if (!FTPReply.isPositiveCompletion(super.getReplyCode())) {
                        return false;
                    }
                }
            }
        }
        return (getReplyCode() == FTPReply.CODE_250 || getReplyCode() == FTPReply.CODE_250);
    }

    /**
     * Conecta ao servidor FTP. Retorna uma confirma��o da conex�o atrav�s de um
     * boolean. TODO: remontar a fun��o para que use somente dados externos a
     * classe
     *
     * @return Valor l�gico que confirma a conex�o.
     */
    @Override
    public boolean connect() {
        boolean conectado = false;

        ftpHost = Util.getProperty("servidorFTP");
        ftpPort = Util.getConfig().getInt("FTPport");

        String ftpProxyHost = Util.getProperty("FTPproxyHost");
        int ftpProxyPort;
        try {
            ftpProxyPort = Util.getConfig().getInt("FTPproxyPort");
        } catch (Exception e) {
            ftpProxyPort = 0;
        }

        Util.log("Iniciando conex�o com " + ftpHost);
        try {
            //TODO: Preparar o suporte a m�ltiplas l�nguas
            FTPClientConfig auxConfig = new FTPClientConfig(FTPClientConfig.SYST_NT);
            configure(auxConfig);
            Util.out.println("Timeout (antes): " + getDefaultTimeout());
            setDefaultTimeout(25000);
            Util.out.println("Timeout (depois): " + getDefaultTimeout());

            //TODO: Testar o acesso via Proxy
            //      usando System.getProperties().put()
            //      http://java.sun.com/j2se/1.5.0/docs/guide/net/properties.html
            if (ftpProxyHost == null && ftpProxyPort != 0) {
                System.getProperties().put("ftp.proxyHost", ftpProxyHost);
                System.getProperties().put("ftp.proxyPort", ftpProxyPort);
            }

            super.connect(ftpHost, ftpPort);
            reply = getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                disconnect("[FtpClient.connect]/ERRO: n�o foi possivel conectar");
                return false;
            }
            Util.log("ok " + ftpHost + " encontrado.. autenticando..");

            SyncEvent ev = null;
            if (syncListener != null) {
                ev = new SyncEvent(this);
            }
            reply = FTPReply.NOT_LOGGED_IN;
            do {

                if (syncListener != null && ev != null) {
                    syncListener.logonStarted(ev);
                }
                Util.log("servidor: " + ftpHost + " em " + ftpRoot + "\nsolicitando conex�o...");

                login(usuario, new String(senha));
                Util.log("usu�rio: " + usuario + " senha: ***");
                reply = getReplyCode();
                retry--;


            } while (reply != FTPReply.USER_LOGGED_IN && retry >= 0);

            if (reply != FTPReply.USER_LOGGED_IN) {
                disconnect("[FtpClient.connect]/ERRO: login/senha incorreto.");
                return conectado;
            } else {
                // conex�o bem sucedida... armazenamos o nome/login
                BancoImagem.getBancoImagem().setUserFTP(getUsuario());
                BancoImagem.getBancoImagem().setPasswordFTP(getSenha());
            }

            Util.log("ok conex�o aceita..");
            // autentica��o ok..

            setFileType(FTPClient.BINARY_FILE_TYPE);
            //ftp.enterRemotePassiveMode();
            // TODO: Achar uma alternativa para realizar o logging do FTP
            //ftp.setLogFile("ftp.log");

            // tenta ir ao diret�rio FTPRoot... caso n�o consiga, tenta criar
            changeWorkingDirectory(ftpRoot);
            if (getReplyCode() != FTPReply.CODE_250) {
                Util.log(ftpRoot + " n�o existe..criando");
                if (makeDirectory(ftpRoot)) {
                    Util.log("[FtpClient.connect]/ERRO: n�o foi poss�vel criar diret�rio " + ftpRoot + " Retorno: " + reply);
                    disconnect("n�o foi poss�vel criar diret�rio");
                    return conectado;
                }
                changeWorkingDirectory(ftpRoot);
                reply = getReplyCode();
                if (reply != FTPReply.CODE_250) {
                    disconnect("[FtpClient.connect]/ERRO: n�o foi poss�vel entrar no diret�rio " + ftpRoot + " que foi rec�m criado.");
                    return conectado;
                }
            }
            conectado = true;
            getSyncListener().connected(new SyncEvent(this));
        } catch (Exception e) {
            conectado = false;
            log.error(e);
            disconnect("[FtpClient.connect]/ERRO: n�o foi possivel manter esta conex�o");
        }

        return conectado;

    }

    /**
     * Desconecta do servidor FTP e apresenta uma mensagem de log.
     *
     * @param msg Mensagem de desconex�o.
     */
    @Override
    public void disconnect(String msg) {
        try {
            Util.log("Desconectando (" + msg + ") ok");
            super.disconnect();
        } catch (Exception e) {
            Util.log("Erro ao tentar desconectar.");
        } finally {
            Util.log(msg);
            try {
                // ao finalizar, verificar se houve erros
                // arquivos com status <> "ok -..." ser�o re-enfileirados
                Iterator iter = listFTP.iterator();
                while (iter.hasNext()) {
                    Arquivo a = new Arquivo((List) iter.next());
                    //Arquivo a=(Arquivo) iter.next();
                    if (!a.getStatus().startsWith("ok")) {
                        CacheFTP.getCache().addCommand(a.getAcao(), a.getAlbumID(), a.getFotoID());
                    }
                }// fim while
            } catch (Exception e) {
            }
            // Dispara o evento de desconnectado
            if (syncListener != null) {
                syncListener.disconnected(new SyncEvent(this));
            }
        }
    }

    /**
     * Faz um load no ArrayList CacheFTP, faz uma busca por itera��o, identifica
     * e carrega as linhas de comandos na seguinte ordem: DELETE, UPLOAD e
     * DOWNLOAD. Carrega esses comandos atrav�s do m�todo
     * {@link net.sf.webphotos.sync.FTP.SyncObject#loadSyncCacheLine() loadSyncCacheLine}().
     * Por �ltimo, ap�s completo o load, limpa a lista do CacheFTP.
     */
    @Override
    public void loadSyncCache() {

        Iterator<ComandoFTP> i = CacheFTP.getCache().iterator();
        String linha;
        Util.out.println("Numero de linhas: " + CacheFTP.getCache().toString());
        // primeiro delete		
        while (i.hasNext()) {
            linha = i.next().toString();
            if (linha.startsWith("3")) {
                loadSyncCacheLine(linha);
            }
        }
        // depois upload
        i = CacheFTP.getCache().iterator();
        while (i.hasNext()) {
            linha = i.next().toString();
            if (linha.startsWith("1")) {
                loadSyncCacheLine(linha);
            }
        }
        // depois download
        i = CacheFTP.getCache().iterator();
        while (i.hasNext()) {
            linha = i.next().toString();
            if (linha.startsWith("2")) {
                loadSyncCacheLine(linha);
            }
        }

        // limpa o CacheFTP
        CacheFTP.getCache().clear();

    }

    /**
     * Recebe uma linha com comando de FTP (DELETE, DOWNLOAD ou UPLOAD),
     * processa o tipo "acao albumID foto" e a carrega em cima do ArrayList
     * listaArquivos, que cont�m dados de
     * {@link net.sf.webphotos.util.Arquivo Arquivo}.
     *
     * @param linha Linha de comando FTP.
     */
    @Override
    public void loadSyncCacheLine(String linha) {
        StringTokenizer tok = new StringTokenizer(linha);
        int acao = -1;
        int albumID = -1;
        int fotoID = -1;

        Util.out.println("carrega: " + linha);

        if (tok.countTokens() == 3) {
            acao = Integer.parseInt(tok.nextToken());
            albumID = Integer.parseInt(tok.nextToken());
            fotoID = Integer.parseInt(tok.nextToken());
        } else {
            // houve um erro...
            Util.out.println("erro: " + linha);
            return;
        }

        // obtem uma lista do �lbum (todos os arquivos)
        File f = new File(getAlbunsRoot(), Integer.toString(albumID));
        String[] ls = f.list();

        switch (acao) {
            // Apagar
            case CacheFTP.DELETE:
            // Receber
            case CacheFTP.DOWNLOAD:
                if (fotoID == 0) {
                    // O �lbum inteiro
                    listaArquivos.add(new Arquivo(linha, acao, albumID, 0, "* todos"));
                } else {
                    // Uma foto
                    listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_a" + fotoID + ".jpg"));
                    listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_b" + fotoID + ".jpg"));
                    listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_c" + fotoID + ".jpg"));
                    listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_d" + fotoID + ".jpg"));
                    if (isEnviarAltaResolucao() == true) {
                        listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, fotoID + ".jpg"));
                        listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, fotoID + ".zip"));
                    }
                    listaArquivos.add(new Arquivo(linha, acao, albumID, 0, albumID + ".xml"));
                    listaArquivos.add(new Arquivo(linha, acao, albumID, 0, albumID + ".js"));
                }
                break;
            // Enviar
            case CacheFTP.UPLOAD:
                if (fotoID == 0) {
                    // O �lbum inteiro
                    Album.getAlbum().loadAlbum(albumID);
                    for (PhotoDTO atual : Album.getAlbum().getFotos()) {
                        fotoID = atual.getFotoID();
                        listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_a" + fotoID + ".jpg"));
                        listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_b" + fotoID + ".jpg"));
                        listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_c" + fotoID + ".jpg"));
                        listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, "_d" + fotoID + ".jpg"));
                        if (isEnviarAltaResolucao() == true) {
                            listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, fotoID + ".jpg"));
                            listaArquivos.add(new Arquivo(linha, acao, albumID, fotoID, fotoID + ".zip"));
                        }
                    }
                    listaArquivos.add(new Arquivo(linha, acao, albumID, 0, albumID + ".xml"));
                    listaArquivos.add(new Arquivo(linha, acao, albumID, 0, albumID + ".js"));
                } else {
                    // Uma foto
                    Util.out.println("Upload alta: " + isEnviarAltaResolucao());
                    for (String fileName : ls) {
                        if ((fileName.startsWith("_") && fileName.toLowerCase().endsWith(fotoID + ".jpg"))
                                || (isEnviarAltaResolucao() && fileName.toLowerCase().endsWith(fotoID + ".zip"))
                                || (isEnviarAltaResolucao() && fileName.toLowerCase().endsWith(fotoID + ".jpg"))) {

                            Arquivo a = new Arquivo(linha, acao, albumID, fotoID, fileName);
                            listaArquivos.add(a);
                        }
                    } // fim for
                }
                break;
        }
    }

    /**
     * Retorna o caminho que deve usar
     *
     * @return Mostra o caminho base
     */
    @Override
    public String getSyncFolder() {
        return ftpRoot;
    }

    /**
     * Determina qual caminho usar
     *
     * @param ftpRoot Par�metro que recebe a informa��o
     */
    @Override
    public void setSyncFolder(String ftpRoot) {
        this.ftpRoot = ftpRoot;
    }

    /**
     * Retorna o ouvinte syncListener.
     *
     * @return Retorna um listener de sincroniza��o.
     */
    @Override
    public SyncListener getSyncListener() {
        return syncListener;
    }

    /**
     * Seta o ouvinte syncListener.
     *
     * @param listener Um listener de sincroniza��o.
     */
    @Override
    public void setSyncListener(SyncListener listener) {
        this.syncListener = listener;
    }

    /**
     * Retorna o objeto copyStreamListener.
     *
     * @return Retorna copyStreamListener.
     */
    @Override
    public CopyStreamListener getCopyStreamListener() {
        return copyStreamListener;
    }

    /**
     * Seta o objeto copyStreamListener.
     *
     * @param copyStreamListener Objeto da classe CopyStreamListener.
     */
    @Override
    public void setCopyStreamListener(CopyStreamListener copyStreamListener) {
        this.copyStreamListener = copyStreamListener;
    }

    /**
     * Retorna o usu�rio.
     *
     * @return Retorna um usu�rio.
     */
    @Override
    public String getUsuario() {
        return usuario;
    }

    /**
     * Seta um nome para usu�rio.
     *
     * @param usuario Usu�rio.
     */
    @Override
    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    /**
     * Retorna a senha do usu�rio.
     *
     * @return Retorna uma senha.
     */
    @Override
    public char[] getSenha() {
        return senha;
    }

    /**
     * Seta uma senha para o usu�rio.
     *
     * @param senha Senha do usu�rio.
     */
    @Override
    public void setSenha(char[] senha) {
        this.senha = senha;
    }

    /**
     * Retorna o ArrayList listaArquivos.
     *
     * @return Retorna listaArquivos.
     */
    @Override
    public ArrayList<Arquivo> getListaArquivos() {
        return listaArquivos;
    }

    /**
     * Seta uma lista para a vari�vel listaArquivos.
     *
     * @param _listaArquivos Lista de arquivos.
     */
    public void setListaArquivos(ArrayList<Arquivo> _listaArquivos) {
        this.listaArquivos = _listaArquivos;
    }

    /**
     * Retorna o valor de enviarAltaResolucao. Especifica se ser�o enviadas ou
     * n�o, as imagens originais.
     *
     * @return Retorna um valor l�gico.
     */
    @Override
    public boolean isEnviarAltaResolucao() {
        return enviarAltaResolucao;
    }

    /**
     * Retorna o diret�rio raiz de alb�ns. TODO: J� existe um m�todo igual a
     * esse na classe Util.
     *
     * @return Retorna um diret�rio.
     */
    public File getAlbunsRoot() {
        return albunsRoot;
    }
}
