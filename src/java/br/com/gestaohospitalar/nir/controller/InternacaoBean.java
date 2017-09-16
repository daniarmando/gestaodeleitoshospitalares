/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.gestaohospitalar.nir.controller;

import br.com.gestaohospitalar.nir.DAO.AltaDAOImpl;
import br.com.gestaohospitalar.nir.DAO.AltaQualificadaDAOImpl;
import br.com.gestaohospitalar.nir.DAO.ConfiguracaoKanbanDAOImpl;
import br.com.gestaohospitalar.nir.DAO.FuncionarioDAOImpl;
import br.com.gestaohospitalar.nir.DAO.HigienizacaoDAOImpl;
import br.com.gestaohospitalar.nir.DAO.InternacaoDAOImpl;
import br.com.gestaohospitalar.nir.DAO.LeitoDAOImpl;
import br.com.gestaohospitalar.nir.DAO.LogDAOImpl;
import br.com.gestaohospitalar.nir.DAO.MedicoDAOImpl;
import br.com.gestaohospitalar.nir.converter.ConverterDataHora;
import br.com.gestaohospitalar.nir.model.Alta;
import br.com.gestaohospitalar.nir.model.AltaQualificada;
import br.com.gestaohospitalar.nir.model.ConfiguracaoKanban;
import br.com.gestaohospitalar.nir.model.Funcionario;
import br.com.gestaohospitalar.nir.model.Higienizacao;
import br.com.gestaohospitalar.nir.model.Internacao;
import br.com.gestaohospitalar.nir.model.Leito;
import br.com.gestaohospitalar.nir.model.Log;
import br.com.gestaohospitalar.nir.model.Medico;
import br.com.gestaohospitalar.nir.model.Paciente;
import br.com.gestaohospitalar.nir.model.enumerator.Status;
import br.com.gestaohospitalar.nir.model.enumerator.TipoLog;
import br.com.gestaohospitalar.nir.model.sigtap.TB_CID;
import br.com.gestaohospitalar.nir.model.sigtap.TB_PROCEDIMENTO;
import br.com.gestaohospitalar.nir.service.DAOException;
import br.com.gestaohospitalar.nir.util.FacesUtil;
import br.com.gestaohospitalar.nir.util.report.GerarRelatorio;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.primefaces.event.SelectEvent;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

/**
 *
 * @author Daniel
 */
@ManagedBean
@SessionScoped
public class InternacaoBean implements Serializable {

    private Internacao internacao;
    private InternacaoDAOImpl daoInternacao;

    private List<Internacao> internacoes = new ArrayList<>();
    private List<Internacao> filtrarLista;

    private List<Medico> medicos = new ArrayList<>();

    private List<Leito> leitos = new ArrayList<>();

    private AltaQualificada altaQualificada = new AltaQualificada();

    private Alta alta = new Alta();

    private Higienizacao higienizacao = new Higienizacao();

    private List<Funcionario> funcionarios = new ArrayList<>();

    //injetando o usuário logado
    @ManagedProperty(value = "#{usuarioBean}")
    private UsuarioBean usuarioBean;

    private LogDAOImpl daoLog;
    private Log log;
    private List<Log> logs = new ArrayList<>();

    private Internacao internacaoSelecionada;
    private Leito leitoSelecionado;

    private Internacao resumoInternacao = new Internacao();

    private Boolean habilitaCampos;

    //variável que define qual o tipo de cadastro vai ser feito 
    //(internação, alta qualificada, alta, higienização)
    private String tipoCadastro = "";

    String msgValidacaoPacienteLog = "", msgValidacaoProcedimentoLog = "", msgValidacaoCidLog = "";

    /**
     * Creates a new instance of InternacaoBean
     */
    public InternacaoBean() {
        this.internacao = new Internacao();
    }

    public void inicializarPaginaPesquisa() {
        this.internacoes = new InternacaoDAOImpl().listar();
    }

    public void inicializarPaginaCadastro() {

        this.medicos = new MedicoDAOImpl().listar();
        this.leitos = new LeitoDAOImpl().listarParaInternacao();
        this.habilitaCampos = !this.leitos.isEmpty();

        //passando a data e hora atual para já vir preenchido no formulário
        this.internacao.setDataEntrada(new Date());
    }

    public String novo() {
        this.internacao = new Internacao();
        return "cadastro-internacao?faces-redirect=true";
    }

    /**
     * método que executa as tarefas necessárias antes de cadastrar a internação
     * usado pela página painel de informações
     */
    public void prepararInternacao() {

        this.internacao = new Internacao();

        //informando o tipo de cadastro para a variável
        this.tipoCadastro = "RI"; //registrar internação

        //listando os médicos
        this.medicos = new MedicoDAOImpl().listar();

        //passando a data e hora atual para já vir preenchido no formulário
        this.internacao.setDataEntrada(new Date());

    }

    /**
     * método que executa as tarefas necessárias antes de cadastrar a alta
     * qualificada usado pela página painel de informações
     */
    public void prepararAltaQualificada() {

        //informando o tipo de cadastro para a variável
        this.tipoCadastro = "RAQ"; //registrar alta qualificada

    }

    /**
     * método que executa as tarefas necessárias antes de cadastrar a alta usado
     * pela página painel de informações
     */
    public void prepararAlta() {

        //informando o tipo de cadastro para a variável
        this.tipoCadastro = "RA"; //registar alta

        //listando os médicos
        this.medicos = new MedicoDAOImpl().listar();

    }

    /**
     * método que executa as tarefas necessárias antes de cadastrar a
     * higienização usado pela página painel de informações
     */
    public void prepararHigienizacao() {

        //informando o tipo de cadastro para a variável
        this.tipoCadastro = "RH"; //registrar higienização

        //listando os funcionários 
        this.funcionarios = new FuncionarioDAOImpl().listar();

    }

    /**
     * método que verifica qual o tipo de cadastro que o usuário selecionou e
     * passa os dados para seus respectivos objetos
     */
    public void prepararCadastro() {
        switch (this.tipoCadastro) {

            case "RI":
                //passando os dados do leito selecionado para a internação
                this.internacao.setLeito(this.leitoSelecionado);
                //limpando o objeto leito selecionado
                this.leitoSelecionado = new Leito();
                break;

            case "RAQ":
                //passando os dados da internação selecionada para a alta qualificada
                this.altaQualificada.setInternacao(this.internacaoSelecionada);
                //limpando o objeto internação selecionada
                this.internacaoSelecionada = new Internacao();
                break;

            case "RA":
                //passando os dados da internação selecionada para a alta
                this.alta.setInternacao(this.internacaoSelecionada);
                //passando os dados do médico da internação selecionada para a alta
                this.alta.setMedico(this.internacaoSelecionada.getMedico());
                //limpando o objeto internação selecionada
                this.internacaoSelecionada = new Internacao();
                break;

            case "RH":
                //passando os dados da internação selecionada para a higienização
                this.higienizacao.setInternacao(this.internacaoSelecionada);
                //limpando o objeto internação selecionada
                this.internacaoSelecionada = new Internacao();
                break;
        }

        //limpando a variável
        this.tipoCadastro = "";

        this.log = new Log();

    }

    /**
     * método que calcula o tempo limite das cores do kanban e salva as
     * informações da internação
     *
     */
    public void registrarInternacao() {
        this.daoInternacao = new InternacaoDAOImpl();

        try {

            //passando o status inicial da internação
            this.internacao.setStatusInternacao(Status.ABERTA.get());

            //passando a chaveMesAno SigTap
            String chaveMesAno = ConverterDataHora.ultimaChaveMesAno();
            this.internacao.setChaveMesAnoProcedimento(chaveMesAno);
            this.internacao.setChaveMesAnoCID(chaveMesAno);

            //pega os valores da configuração kanban para fazer o cálculo
            ConfiguracaoKanban configuracaoKanban = new ConfiguracaoKanbanDAOImpl().configuracaoKanbanPorId(1);

            //converte a data de entrada para o tipo LocalDateTime para fazer o cálculo
            LocalDateTime dataEntrada = ConverterDataHora.paraLocalDateTime(this.internacao.getDataEntrada());

            //calculando e setando limite de tempo para cor verde
            Long verde = Math.round(((this.internacao.getProcedimento().getQT_DIAS_PERMANENCIA() * 24)
                    * configuracaoKanban.getValorVerdeKanban()) / 100.0);
            LocalDateTime tempoLimiteVerde = dataEntrada.plusHours(verde);
            this.internacao.setDataHoraLimiteVerde(ConverterDataHora.paraDate(tempoLimiteVerde));

            //calculando e setando limite de tempo para cor amarelo
            Long amarelo = verde + Math.round(((this.internacao.getProcedimento().getQT_DIAS_PERMANENCIA() * 24)
                    * configuracaoKanban.getValorAmareloKanban()) / 100.0);
            LocalDateTime tempoLimiteAmarelo = dataEntrada.plusHours(amarelo);
            this.internacao.setDataHoraLimiteAmarelo(ConverterDataHora.paraDate(tempoLimiteAmarelo));

            //calculando e setando limite de tempo para cor vermelho
            Long vermelho = Long.valueOf(this.internacao.getProcedimento().getQT_DIAS_PERMANENCIA() * 24);
            LocalDateTime tempoLimiteVermelho = dataEntrada.plusHours(vermelho);
            this.internacao.setDataHoraLimiteVermelho(ConverterDataHora.paraDate(tempoLimiteVermelho));

            //salvando a internação
            this.daoInternacao.salvar(this.internacao);

            //salvando o log
            this.log.setTipo(TipoLog.REGISTRAR_INTERNACAO.get());
            this.log.setIdObjeto(this.internacao.getIdInternacao());
            salvarLog();

            //se gerou mensagem de validação ao selecionar o paciente salva um log
            if (this.msgValidacaoPacienteLog.length() > 0) {
                this.log = new Log();
                this.log.setTipo(TipoLog.ERRO_VALIDACAO_PACIENTE.get());
                this.log.setIdObjeto(this.internacao.getIdInternacao());
                this.log.setDetalhe(this.msgValidacaoPacienteLog);
                salvarLog();
            }

            //se gerou mensagem de validação ao selecionar o procedimento salva um log
            if (this.msgValidacaoProcedimentoLog.length() > 0) {
                this.log = new Log();
                this.log.setTipo(TipoLog.ERRO_VALIDACAO_PROCEDIMENTO.get());
                this.log.setIdObjeto(this.internacao.getIdInternacao());
                this.log.setDetalhe(this.msgValidacaoProcedimentoLog);
                salvarLog();
            }

            //se gerou mensagem de validação ao selecionar o cid salva um log
            if (this.msgValidacaoCidLog.length() > 0) {
                this.log = new Log();
                this.log.setTipo(TipoLog.ERRO_VALIDACAO_CID.get());
                this.log.setIdObjeto(this.internacao.getIdInternacao());
                this.log.setDetalhe(this.msgValidacaoCidLog);
                salvarLog();
            }

            //atualizando a lista de leitos
            this.leitos = new LeitoDAOImpl().listarParaInternacao();
            this.habilitaCampos = !this.leitos.isEmpty();

            //envia mensagem para usuários conectados e atualiza a página painel de informações
            notificar(TipoLog.REGISTRAR_INTERNACAO.get(), this.internacao);

            this.internacao = new Internacao();
            this.medicos = new ArrayList<>();
            this.msgValidacaoPacienteLog = "";
            this.msgValidacaoProcedimentoLog = "";
            this.msgValidacaoCidLog = "";

        } catch (DAOException e) {
            FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }

    }

    /**
     * método que salva a alta qualificada
     *
     */
    public void registrarAltaQualificada() {
        AltaQualificadaDAOImpl daoAltaQualificada = new AltaQualificadaDAOImpl();

        try {

            //se a data da previsão de alta informada for igual a data de entrada da internação
            if (this.altaQualificada.getDataHoraPrevisao().compareTo(this.altaQualificada.getInternacao().getDataEntrada()) == 0) {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A data da previsão de alta não pode ser igual que a data de entrada da internação!");
                //se a data da previsão de alta informada for menor que a data de entrada da internação
            } else if (this.altaQualificada.getDataHoraPrevisao().before(this.altaQualificada.getInternacao().getDataEntrada())) {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A data da previsão de alta não pode ser menor que a data de entrada da internação!");
            } else {

                //passando a data e hora atual
                this.altaQualificada.setDataHoraInformacao(new Date());

                //salvando a alta qualificada
                daoAltaQualificada.salvar(this.altaQualificada);

                //salvando o log
                this.log.setTipo(TipoLog.REGISTRAR_ALTA_QUALIFICADA.get());
                this.log.setIdObjeto(this.altaQualificada.getInternacao().getIdInternacao());
                this.log.setDetalhe("alta qualiificada código " + this.altaQualificada.getIdAltaQualificada() + ".");
                salvarLog();

                //envia mensagem para usuários conectados e atualiza a página painel de informações
                notificar(TipoLog.REGISTRAR_ALTA_QUALIFICADA.get(), this.altaQualificada.getInternacao());

                this.altaQualificada = new AltaQualificada();
            }
        } catch (DAOException e) {
            FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    /**
     * método que salva a alta
     *
     */
    public void registrarAlta() {
        AltaDAOImpl daoAlta = new AltaDAOImpl();

        try {

            //se a data da realização da alta informada for igual a data de entrada da internação
            if (this.alta.getDataHoraRealizacao().compareTo(this.alta.getInternacao().getDataEntrada()) == 0) {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A data da alta não pode ser igual que a data de entrada da internação!");
                //se a data da previsão de alta informada for menor que a data de entrada da internação
            } else if (this.alta.getDataHoraRealizacao().before(this.alta.getInternacao().getDataEntrada())) {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A data da alta não pode ser menor que a data de entrada da internação!");
            } else {

                //buscando alta qualificada para a internação
                this.altaQualificada = new AltaQualificadaDAOImpl().altaQualificadaPorIdInternacao(this.alta.getInternacao().getIdInternacao());

                //se tiver alta qualificada para a internação, passa o idAltaQualificada para a alta
                if (this.altaQualificada != null) {
                    this.alta.setAltaQualificada(this.altaQualificada);
                }

                //passando a data e hora atual
                this.alta.setDataHoraInformacao(new Date());

                //salvando a alta
                daoAlta.salvar(this.alta);

                //salvando o log
                this.log.setTipo(TipoLog.REGISTRAR_ALTA.get());
                this.log.setIdObjeto(this.alta.getInternacao().getIdInternacao());
                this.log.setDetalhe("alta código " + this.alta.getIdAlta() + ".");
                salvarLog();

                //envia mensagem para usuários conectados e atualiza a página painel de informações
                notificar(TipoLog.REGISTRAR_ALTA.get(), this.alta.getInternacao());

                this.alta = new Alta();
                this.altaQualificada = new AltaQualificada();
                this.medicos = new ArrayList<>();
            }
        } catch (DAOException e) {
            FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }

    }

    /**
     * método que atualiza a internação salvando a saída do paciente do leito
     *
     */
    public void registrarSaida() {
        this.daoInternacao = new InternacaoDAOImpl();

        try {

            //atualizando a internação com a data e hora da saída e alterando o status para higienização
            this.internacaoSelecionada.setStatusInternacao(Status.HIGIENIZACAO.get());
            this.daoInternacao.salvar(this.internacaoSelecionada);

            //salvando o log
            this.log.setTipo(TipoLog.REGISTRAR_SAIDA.get());
            this.log.setIdObjeto(this.internacaoSelecionada.getIdInternacao());
            salvarLog();

            //envia mensagem para usuários conectados e atualiza a página painel de informações
            notificar(TipoLog.REGISTRAR_SAIDA.get(), this.internacaoSelecionada);

            this.internacaoSelecionada = new Internacao();

        } catch (DAOException e) {
            FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }

    }

    /**
     * método que salva a higienização do leito
     *
     */
    public void registrarHigienizacao() {
        HigienizacaoDAOImpl daoHigienizacao = new HigienizacaoDAOImpl();

        try {

            //se a data inicial for menor que a data de saída do paciente do leito
            if (higienizacao.getDataHoraInicio().before(higienizacao.getInternacao().getDataSaidaLeito())) {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A data e hora da higienização não pode ser menor que a data da saída do paciente!");
                //se a data inicial for igual a final
            } else if (higienizacao.getDataHoraInicio().compareTo(higienizacao.getDataHoraFim()) == 0) {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A data e hora de início não pode ser igual a data e hora final!");
                //se a data inicial for maior que a data final
            } else if (higienizacao.getDataHoraInicio().after(higienizacao.getDataHoraFim())) {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A data e hora de início não pode ser maior que a data e hora final!");
            } else {
                //salvando a higienização
                this.higienizacao.setTempoHigienizacaoMinutos(ConverterDataHora.diferencaEmMinutos(this.higienizacao.getDataHoraInicio(), this.higienizacao.getDataHoraFim()));
                daoHigienizacao.salvar(this.higienizacao);

                //salvando o log
                this.log.setTipo(TipoLog.REGISTRAR_HIGIENIZACAO.get());
                this.log.setIdObjeto(this.higienizacao.getInternacao().getIdInternacao());
                this.log.setDetalhe("higienização código " + this.higienizacao.getIdHigienizacao() + ".");
                salvarLog();

                //envia mensagem para usuários conectados e atualiza a página painel de informações
                notificar(TipoLog.REGISTRAR_HIGIENIZACAO.get(), this.higienizacao.getInternacao());

                this.higienizacao = new Higienizacao();
                this.funcionarios = new ArrayList<>();
            }
        } catch (DAOException e) {
            FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }

    }

    /**
     * método que cancela a internação
     *
     * @param internacao
     */
    public void cancelarInternacao(Internacao internacao) {
        this.daoInternacao = new InternacaoDAOImpl();

        try {

            //se a internação não tiver alta
            if (internacao.getDataAlta() == null) {

                //passando o status cancelada para a internação
                internacao.setStatusInternacao(Status.CANCELADA.get());

                //salvando a internação
                this.daoInternacao.salvar(internacao);

                //salvando o log
                this.log = new Log();
                this.log.setTipo(TipoLog.CANCELAR_INTERNACAO.get());
                this.log.setIdObjeto(internacao.getIdInternacao());
                salvarLog();

                //envia mensagem para usuários conectados e atualiza a página painel de informações
                notificar(TipoLog.CANCELAR_INTERNACAO.get(), internacao);

            } else {
                FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Internação não pode mais ser cancelada.");
            }

            this.internacao = new Internacao();
        } catch (DAOException e) {
            FacesUtil.adicionarMensagem(FacesMessage.SEVERITY_ERROR, e.getMessage());
        }
    }

    /**
     * método para notificar usuários conectados
     *
     * @param msg
     * @param internacao
     *
     */
    public void notificar(String msg, Internacao internacao) {

        msg += ". Setor: " + internacao.getLeito().getQuarto().getSetor().getIdSetor() +
               ". Quarto: " + internacao.getLeito().getQuarto().getIdQuarto() + 
               ". Leito: " + internacao.getLeito().getIdLeito() + 
               ".";
        
        String detalhe = "Feita por: " + this.usuarioBean.getUsuario().getLogin() + ".";
        
        String CANAL = "/notificar";

        EventBus eventBus = EventBusFactory.getDefault().eventBus();
        eventBus.publish(CANAL, new FacesMessage(msg, detalhe));

    }

    /**
     * método que monta e salva o log
     *
     */
    public void salvarLog() {
        this.daoLog = new LogDAOImpl();

        //passando as demais informações 
        this.log.setDataHora(new Date());
        this.log.setObjeto("internacao");
        this.log.setUsuario(this.usuarioBean.getUsuario());
        //salvando o log
        this.daoLog.salvar(this.log);
    }

    /**
     * método que exibe o último log na página
     *
     * @return
     */
    public String ultimoLog() {
        this.log = new LogDAOImpl().ultimoLogPorObjeto("internacao");
        return this.log != null ? "Última modificação feita em " + ConverterDataHora.formatarDataHora(this.getLog().getDataHora()) + " por " + this.getLog().getUsuario().getLogin() + "." : "";
    }

    /**
     * método que traz os logs para do objeto selecionado
     */
    public void gerarLogs() {
        this.logs = new LogDAOImpl().listarPorIdObjeto("internacao", this.internacao.getIdInternacao());
    }

    /**
     * Método que gera o relatório passando uma lista e o nome do relatório para
     * a classe de relatórios genéricos
     */
    public void gerarRelatorio() {
        String nomeRelatorio = "relatorioInternacao";
        GerarRelatorio relatorio = new GerarRelatorio();

        List<Object> listaObjetos = new ArrayList<>(this.internacoes);
        relatorio.getRelatorio(listaObjetos, nomeRelatorio);
    }

    /**
     * método que busca internação por id
     *
     * @param idInternacao
     */
    public void internacaoPorId(Integer idInternacao) {
        this.daoInternacao = new InternacaoDAOImpl();

        //buscando a internação para mostrar um resumo dela por meio do notificationBar
        this.resumoInternacao = this.daoInternacao.internacaoPorId(idInternacao);
    }

    /**
     * método que pega o paciente selecionado pelo evento dialogReturn
     *
     * @param event
     */
    public void pacienteSelecionado(SelectEvent event) {
        //pegando o paciente selecionado
        Map<String, Paciente> pacienteSelecionado = (Map<String, Paciente>) event.getObject();

        Paciente paciente = new Paciente();

        //percorrendo o map para pegar o paciente e a mensagem de validação para o log
        for (Map.Entry<String, Paciente> p : pacienteSelecionado.entrySet()) {
            this.msgValidacaoPacienteLog = p.getKey();
            paciente = p.getValue();
        }

        this.internacao.setPaciente(paciente);
    }

    /**
     * método que pega o procedimento selecionado pelo evento dialogReturn
     *
     * @param event
     */
    public void procedimentoSelecionado(SelectEvent event) {
        //pegando o procedimento selecionado
        Map<String, TB_PROCEDIMENTO> procedimentoSelecionado = (Map<String, TB_PROCEDIMENTO>) event.getObject();

        TB_PROCEDIMENTO procedimento = new TB_PROCEDIMENTO();

        //percorrendo o map para pegar o procedimento e a mensagem de validação para o log
        for (Map.Entry<String, TB_PROCEDIMENTO> p : procedimentoSelecionado.entrySet()) {
            this.msgValidacaoProcedimentoLog = p.getKey();
            procedimento = p.getValue();
        }

        this.internacao.setProcedimento(procedimento);
    }

    /**
     * método que pega o CID selecionado pelo evento dialogReturn
     *
     * @param event
     */
    public void cidSelecionado(SelectEvent event) {
        //pegando o cid selecionado
        Map<String, TB_CID> cidSelecionado = (Map<String, TB_CID>) event.getObject();

        TB_CID cid = new TB_CID();

        //percorrendo o map para pegar o cid e a mensagem de validação para o log
        for (Map.Entry<String, TB_CID> c : cidSelecionado.entrySet()) {
            this.msgValidacaoCidLog = c.getKey();
            cid = c.getValue();
        }

        this.internacao.setCid(cid);

    }

    /**
     * @return the medicos
     */
    public List<Medico> getMedicos() {
        return medicos;
    }

    /**
     * @param medicos the medicos to set
     */
    public void setMedicos(List<Medico> medicos) {
        this.medicos = medicos;
    }

    /**
     * @return the leitos
     */
    public List<Leito> getLeitos() {
        return leitos;
    }

    /**
     * @param leitos the leitos to set
     */
    public void setLeitos(List<Leito> leitos) {
        this.leitos = leitos;
    }

    /**
     * @return the internacao
     */
    public Internacao getInternacao() {
        return internacao;
    }

    /**
     * @param internacao the internacao to set
     */
    public void setInternacao(Internacao internacao) {
        this.internacao = internacao;
    }

    /**
     * @return "Descrição do CID selecionado"
     */
    public String getDescricaoCid() {
        if (this.internacao == null) {
            this.internacao = new Internacao();
        }
        return this.internacao.getCid() == null ? null : this.internacao.getCid().getCO_CID() + " - " + this.internacao.getCid().getNO_CID();
    }

    /**
     * @param descricaoCid
     */
    public void setDescricaoCid(String descricaoCid) {
    }

    /**
     * @return "Descrição do Paciente selecionado"
     */
    public String getDescricaoPaciente() {
        if (this.internacao == null) {
            this.internacao = new Internacao();
        }
        return this.internacao.getPaciente() == null ? null : this.internacao.getPaciente().getNomePessoa();
    }

    /**
     * @param descricaoPaciente
     */
    public void setDescricaoPaciente(String descricaoPaciente) {
    }

    /**
     * @return "Descrição do Procedimento selecionado"
     */
    public String getDescricaoProcedimento() {
        if (this.internacao == null) {
            this.internacao = new Internacao();
        }
        return this.internacao.getProcedimento() == null ? null : this.internacao.getProcedimento().getCO_PROCEDIMENTO() + " - " + this.internacao.getProcedimento().getNO_PROCEDIMENTO();
    }

    /**
     * @param descricaoProcedimento
     */
    public void setDescricaoProcedimento(String descricaoProcedimento) {
    }

    /**
     * @return the internacoes
     */
    public List<Internacao> getInternacoes() {
        return internacoes;
    }

    /**
     * @return the filtrarLista
     */
    public List<Internacao> getFiltrarLista() {
        return filtrarLista;
    }

    /**
     * @param filtrarLista the filtrarLista to set
     */
    public void setFiltrarLista(List<Internacao> filtrarLista) {
        this.filtrarLista = filtrarLista;
    }

    /**
     * @return the internacaoSelecionada
     */
    public Internacao getInternacaoSelecionada() {
        return internacaoSelecionada;
    }

    /**
     * @param internacaoSelecionada the internacaoSelecionada to set
     */
    public void setInternacaoSelecionada(Internacao internacaoSelecionada) {
        this.internacaoSelecionada = internacaoSelecionada;

        //chamando método para terminar de prepara o cadastro que o usuário selecionou
        prepararCadastro();
    }

    /**
     * @return the higienizacao
     */
    public Higienizacao getHigienizacao() {
        return higienizacao;
    }

    /**
     * @param higienizacao the higienizacao to set
     */
    public void setHigienizacao(Higienizacao higienizacao) {
        this.higienizacao = higienizacao;
    }

    /**
     * @return the funcionarios
     */
    public List<Funcionario> getFuncionarios() {
        return funcionarios;
    }

    /**
     * @param funcionarios the funcionarios to set
     */
    public void setFuncionarios(List<Funcionario> funcionarios) {
        this.funcionarios = funcionarios;
    }

    /**
     * @return the leitoSelecionado
     */
    public Leito getLeitoSelecionado() {
        return leitoSelecionado;
    }

    /**
     * @param leitoSelecionado the leitoSelecionado to set
     */
    public void setLeitoSelecionado(Leito leitoSelecionado) {
        this.leitoSelecionado = leitoSelecionado;

        //chamando método para terminar de prepara o cadastro que o usuário selecionou
        prepararCadastro();
    }

    /**
     * @return the resumoInternacao
     */
    public Internacao getResumoInternacao() {
        return resumoInternacao;
    }

    /**
     * @param resumoInternacao the resumoInternacao to set
     */
    public void setResumoInternacao(Internacao resumoInternacao) {
        this.resumoInternacao = resumoInternacao;
    }

    /**
     * @return the habilitaCampos
     */
    public Boolean getHabilitaCampos() {
        return habilitaCampos;
    }

    /**
     * @return the altaQualificada
     */
    public AltaQualificada getAltaQualificada() {
        return altaQualificada;
    }

    /**
     * @param altaQualificada the altaQualificada to set
     */
    public void setAltaQualificada(AltaQualificada altaQualificada) {
        this.altaQualificada = altaQualificada;
    }

    /**
     * @return the alta
     */
    public Alta getAlta() {
        return alta;
    }

    /**
     * @param alta the alta to set
     */
    public void setAlta(Alta alta) {
        this.alta = alta;
    }

    //setter necessario para a anotação @ManagedProperty funcionar corretamente
    public void setUsuarioBean(UsuarioBean usuarioBean) {
        this.usuarioBean = usuarioBean;
    }

    /**
     * @return the logs
     */
    public List<Log> getLogs() {
        return logs;
    }

    /**
     * @return the log
     */
    public Log getLog() {
        return log;
    }

    /**
     * @param log the log to set
     */
    public void setLog(Log log) {
        this.log = log;
    }

}
