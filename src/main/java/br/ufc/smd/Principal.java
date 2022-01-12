package br.ufc.smd;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

public class Principal {

	// Pacientes
	static DefaultTableModel modeloTabelaPaciente = new DefaultTableModel();
	static final JTable tabelaPaciente = new JTable(modeloTabelaPaciente);
	static TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(tabelaPaciente.getModel());

	// Eventos
	static DefaultTableModel modeloTabelaEvento = new DefaultTableModel();
	static JTable tabelaEvento = new JTable(modeloTabelaEvento);

	// Alertas
	static DefaultTableModel modeloTabelaAlerta = new DefaultTableModel();

	// Outros
	static SimpleDateFormat formatadorData = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	public static void main(String[] args) {
		try {

			// Início - "Outras variáveis"
			JFrame frame = new JFrame();
			JTabbedPane jTabbedPaneContainer = new JTabbedPane();

			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints gbc = new GridBagConstraints();

			JPanel panelBuscaPaciente = new JPanel();
			JPanel panelListaPacientes = new JPanel();
			JPanel panelPaciente = new JPanel();

			JPanel panelEventos = new JPanel();

			JPanel panelCriaAlerta = new JPanel();
			JPanel panelListaAlertas = new JPanel();
			JPanel panelAlerta = new JPanel();
			// Fim - "Outras variáveis"


			// Início - Configuração da conexão com a base de dados
			FileInputStream serviceAccount = new FileInputStream(
					"src/main/resources/diario-sono-5a1db-firebase-adminsdk-5z2p8-72da99c367.json");

			FirebaseOptions options = new FirebaseOptions.Builder()
					                                     .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
			FirebaseApp.initializeApp(options);
			final Firestore dbFirestore = FirestoreClient.getFirestore();
			// Fim - Configuração da conexão com a base de dados
			

			// Início - Aba Pacientes - Busca
			JLabel labelBuscaPaciente = new JLabel();
			labelBuscaPaciente.setText("Busca ");
			final JTextField textBuscaPaciente = new JTextField(null, 24);

			textBuscaPaciente.getDocument().addDocumentListener(new DocumentListener(){
	            public void insertUpdate(DocumentEvent e) {
	                String text = textBuscaPaciente.getText();

	                if (text.trim().length() == 0) {
	                    rowSorter.setRowFilter(null);
	                } else {
	                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
	                }
	            }

	            public void removeUpdate(DocumentEvent e) {
	                String text = textBuscaPaciente.getText();

	                if (text.trim().length() == 0) {
	                    rowSorter.setRowFilter(null);
	                } else {
	                    rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
	                }
	            }

	            public void changedUpdate(DocumentEvent e) {
	                throw new UnsupportedOperationException("Não suportado...");
	            }
	        });
			
			tabelaPaciente.setRowSorter(rowSorter);
			// Fim - Aba Pacientes - Busca


			// Início - Aba Pacientes - Listagem
			CollectionReference collectionReferencePacientes = dbFirestore.collection("usuarios");
			Iterable<DocumentReference> iterator = collectionReferencePacientes.listDocuments();

			modeloTabelaPaciente.addColumn("Usuário");
			modeloTabelaPaciente.addColumn("Nome");
			modeloTabelaPaciente.addColumn("Telefone");
			modeloTabelaPaciente.addColumn("CPF");
			modeloTabelaPaciente.addColumn("Dt. Nascimento");

			for (DocumentReference docPaciente : iterator) {
				Date dataNascimento = docPaciente.get().get().getDate("dataNascimento");
				
				Object[] pacientes = {docPaciente.get().get().getString("usuario"),  docPaciente.get().get().getString("nome"),
						              docPaciente.get().get().getString("telefone"), docPaciente.get().get().getString("cpf"),
						              formatadorData.format(dataNascimento)};
				modeloTabelaPaciente.addRow(pacientes);
			}
			// Fim - Aba Pacientes - Listagem


			// Início - Aba Eventos - Listagem
			modeloTabelaEvento.addColumn("Tipo de evento");
			modeloTabelaEvento.addColumn("Sub-evento");
			modeloTabelaEvento.addColumn("Momento");
			modeloTabelaEvento.addColumn("Duração");
			modeloTabelaEvento.addColumn("Observação");

			tabelaPaciente.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			    public void valueChanged(ListSelectionEvent event) {
			        if (tabelaPaciente.getSelectedRow() > -1) {
			            String usuarioSelecionado = tabelaPaciente.getValueAt(tabelaPaciente.getSelectedRow(), 0).toString();

			            CollectionReference collectionReferenceEventos = dbFirestore.collection("usuarios").document(usuarioSelecionado).collection("eventos");
			            Iterable<DocumentReference> iteratorEventos = collectionReferenceEventos.listDocuments();

			            if (modeloTabelaEvento.getRowCount() > 0) {
			                for (int i = modeloTabelaEvento.getRowCount() - 1; i > -1; i--) {
			                	modeloTabelaEvento.removeRow(i);
			                }
			            }

						for (DocumentReference docEvento : iteratorEventos) {
							try {
								Date dataMomento = docEvento.get().get().getDate("momento");

								Object[] eventos = { docEvento.get().get().getString("tipoEvento"),
													 docEvento.get().get().getString("subEvento"),
													 formatadorData.format(dataMomento),
													 docEvento.get().get().getString("duracao"),
													 docEvento.get().get().getString("observacao")
												   };
								modeloTabelaEvento.addRow(eventos);
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
						}
			        }
			    }
			});
			// Fim - Aba Eventos - Listagem


			// Alertas - Início - Cadastro
			JLabel labelDescricaoAlerta = new JLabel();
			labelDescricaoAlerta.setText("Descrição ");
			final JTextField textDescricaoAlerta = new JTextField(null, 24);

			JButton buttonSalvarAlerta = new JButton();
			buttonSalvarAlerta.setText("Adicionar");
			buttonSalvarAlerta.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					Map<String, Object> camposAlerta = new HashMap<String, Object>(3);
					camposAlerta.put("dataCadastro", Timestamp.now());
					camposAlerta.put("descricao", textDescricaoAlerta.getText());
					camposAlerta.put("habilitado", Boolean.TRUE);

					ApiFuture<DocumentReference> docApiFuture = dbFirestore.collection("alertas").add(camposAlerta);

					Date dataCadastro = ((Timestamp) camposAlerta.get("dataCadastro")).toDate();

					Object[] alerta = { formatadorData.format(dataCadastro),
										camposAlerta.get("descricao"),
										camposAlerta.get("habilitado")
									  };
					modeloTabelaAlerta.addRow(alerta);
					textDescricaoAlerta.setText("");
			    }
			});
			// Alertas - Fim - Cadastro


			// Alertas - Início - Listagem
			modeloTabelaAlerta.addColumn("Data de cadastro");
			modeloTabelaAlerta.addColumn("Descrição");
			modeloTabelaAlerta.addColumn("Habilitado");
			modeloTabelaAlerta.fireTableDataChanged();

			CollectionReference collectionReferenceAlertas = (CollectionReference) dbFirestore.collection("alertas");
			Iterable<DocumentReference> iteratorAlertas = collectionReferenceAlertas.listDocuments();

			for (DocumentReference docAlerta : iteratorAlertas) {
				try {
					Date dataCadastro = docAlerta.get().get().getDate("dataCadastro");

					Object[] alertas = { formatadorData.format(dataCadastro),
										 docAlerta.get().get().getString("descricao"),
										 docAlerta.get().get().getBoolean("habilitado")
									   };
					modeloTabelaAlerta.addRow(alertas);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			final JTable tabelaAlerta = new JTable(modeloTabelaAlerta);
			// Alertas - Fim - Listagem


			// Montagem dos painéis e tela
			panelBuscaPaciente.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),  "Buscar paciente", TitledBorder.CENTER, TitledBorder.TOP));
			panelListaPacientes.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Pacientes",       TitledBorder.CENTER, TitledBorder.TOP));

			panelEventos.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),        "Eventos",         TitledBorder.CENTER, TitledBorder.TOP));

			panelCriaAlerta.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),     "Criar alerta",    TitledBorder.CENTER, TitledBorder.TOP));
			panelListaAlertas.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),   "Alertas",         TitledBorder.CENTER, TitledBorder.TOP));


			panelListaPacientes.add(new JScrollPane(tabelaPaciente));
			panelEventos.add(new JScrollPane(tabelaEvento));
			panelListaAlertas.add(new JScrollPane(tabelaAlerta));


			panelBuscaPaciente.setLayout(layout);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx = 0;
			gbc.gridy = 0;
			panelBuscaPaciente.add(labelBuscaPaciente, gbc);

			gbc.gridx = 1;
			gbc.gridy = 0;
			panelBuscaPaciente.add(textBuscaPaciente, gbc);

			panelPaciente.add(panelBuscaPaciente);
			panelPaciente.add(panelListaPacientes);


			panelCriaAlerta.setLayout(layout);

			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx = 0;
			gbc.gridy = 0;
			panelCriaAlerta.add(labelDescricaoAlerta, gbc);

			gbc.gridx = 1;
			gbc.gridy = 0;
			panelCriaAlerta.add(textDescricaoAlerta, gbc);

			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 2;
			panelCriaAlerta.add(buttonSalvarAlerta, gbc);

			panelAlerta.add(panelCriaAlerta);
			panelAlerta.add(panelListaAlertas);

			jTabbedPaneContainer.add("Pacientes", panelPaciente);
			jTabbedPaneContainer.add("Eventos",   panelEventos);
			jTabbedPaneContainer.add("Alertas",   panelAlerta);

			frame.add(jTabbedPaneContainer);
			frame.setTitle("Diário do sono - cliente");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			frame.setVisible(true);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}