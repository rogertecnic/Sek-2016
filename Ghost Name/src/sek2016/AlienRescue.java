package sek2016;

import java.util.List;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.utility.Delay;
import sek2016.Celula.Status;

/**
 * 
 * @author Equipe Sek 2016:<br>
 *         Diego Costa - Eng. da Computacao<br>
 *         Karinny Gol�alves<br>
 *         Lucas Sim�es - Eng. da Computacao<br>
 *         Mariana Moreno - Eng. Mec�nica<br>
 *         Rog�rio Pereira Batista - Eng. El�trica<br>
 */
public class AlienRescue implements Runnable {

	enum Module {
		Central, Cave, Obstacle, OutOfModule;
	}
	// =================INSTANCIAS DAS THREADS=================================

	/**
	 * Thread que comanda a execu��o do PID
	 */
	public static Thread threadPID;
	/**
	 * Thread que permite que a tacometria seja usada
	 */
	private static Thread threadTacometria;

	// =========================CONSTANTES DE PROCESSO=========================
	/**
	 * quantidade de colunas na matriz
	 */
	private static final int COL_AMT = 9;
	/**
	 * quantidade de linhas na matriz
	 */
	private static final int LIN_AMT = 9;

	// =======================VARIAVEIS DE MAPA===========================
	public final static Celula[][] CENTRAL_MAP = new Celula[LIN_AMT][COL_AMT];
	public final static Celula[][] CAVE_MAP = new Celula[LIN_AMT][COL_AMT];
	public final static Celula[][] OBSTACLE_MAP = new Celula[LIN_AMT][COL_AMT];

	private static Posicao inputCell = new Posicao(0, 4);
	private static Posicao caveEntrance;
	private static Posicao caveExit;
	private static Posicao obstacleEntrace;
	private static Posicao obstacleExit;

	private static Astar aStar;

	// ======================== Vari�veis de posicionamento=================

	/**
	 * Indica qual m�dulo o robo se encontra
	 */
	private static Module modulo = Module.OutOfModule;

	/**
	 * Armazena a orienta��o do robo quando ele troca de modulo
	 */
	private static int orientacaoArmazenada;

	/**
	 * Distancia da travessia entre um m�dulo e outro ou de entrada no modulo
	 * central.
	 */
	private static final float DIST_TRAVESSIA = 0.25f;
	private static final float DIST_SAIDA = 0.4F;
	private static final float DIST_ENTRADA = 0.5F;

	// =================================Flags de uso geral=================

	/**
	 * Variavel global que indica se a Thread do programa est� executando (ON)
	 * ou fechada (OFF)
	 */
	public static boolean alienRescueON;

	/**
	 * Flag que informa que houve uma mudan�a de c�lula
	 */
	public static boolean cellExchanged = false;

	/**
	 * Flag que informa que a leitura pra aquela c�lula j� foi feita
	 */
	public static boolean cellAlreadyRead = false;

	public static boolean captured = false;

	// ==================== Caminho do robo============================
	/**
	 * Lista ligada que contem o caminho at� algo
	 */
	private static List<Celula> path;

	// =====================Pontos de controle dos 3 mapas=================
	// --------------------Mapa central-------------------------

	private static Posicao point1CentralMap = new Posicao(4, 4);
	private static Posicao point2CentralMap = new Posicao(4, 7);
	private static Posicao point3CentralMap = new Posicao(7, 4);
	private static Posicao point4CentralMap = new Posicao(2, 1);

	// --------------------Mapa da caverna----------------------

	private static Posicao point1Cavemap = new Posicao(4, 7);
	private static Posicao point2Cavemap = new Posicao(7, 4);
	private static Posicao point3Cavemap = new Posicao(4, 1);

	// -----------------Mapa do obst�culo----------------------
	// ----------------Se��o 1----------------------

	private static Posicao point1ObstacleMapS1 = new Posicao(1, 6);
	private static Posicao point2ObstacleMapS1 = new Posicao(1, 2);

	// --------Se��o 2-----------------------------

	private static Posicao point4ObstacleMapS2 = new Posicao(3, 2);
	private static Posicao point5ObstacleMapS2 = new Posicao(3, 6);

	// -----------Se��o 3------------------------
	private static Posicao point7ObstacleMapS3 = new Posicao(6, 7);
	private static Posicao point9ObstacleMapS3 = new Posicao(6, 5);
	private static Posicao point10ObstacleMapS3 = new Posicao(7, 2);

	// ========================M�todos de Implementa��o====================
	/**
	 * Metodo que rege todo o codigo do robo
	 */
	@Override
	public void run() {
		Navigation.garraFechada = false;
		try { // o codigo deve ficar dentro desse try
				// ======INICIO DO CODIGO===================================
			/*
			 * Thread da PID � iniciada aqui.
			 */
			threadPID = new Thread(new PID());
			threadPID.setDaemon(true);
			threadPID.setName("threadPID");
			PID.pidRunning = true;
			threadPID.start();

			/*
			 * Thread da Tacometria � iniciada aqui.
			 */
			threadTacometria = new Thread(new Navigation());
			threadTacometria.setDaemon(true);
			threadTacometria.setName("Thread Tacometria");
			threadTacometria.start();

			Navigation.openGarra();
			toRescue();

			// ======FINAL DO CODIGO=========================================

			alienRescueON = false;

			/*
			 * Quando o menu � chamado, essa thread � desligada e essa exce��o �
			 * lan�ada
			 */
		} catch (ThreadDeath e) {

			e.getStackTrace();

		} catch (Exception e) {

			e.getStackTrace();
			// Sound.buzz();

		}
	}

	/**
	 * M�todo Motherfucker que siplemesmente faz o resgate
	 * 
	 * @throws Exception
	 */
	public static void toRescue() throws Exception {
		/*
		 * Leitura do ch�o e defini��o do que resgatar
		 */

		CENTRAL_MAP[inputCell.x][inputCell.y].setStatus(Status.empty);
		if (modulo == Module.OutOfModule) {

			enterModule(Module.Central);

		}
		switch ( bestPlaceToSearch() /* Module.Cave*/) {

		case Central:

			centralMapStrategy();
			break;

		case Cave:

			setPath(caveEntrance);
			if (goTo(getPath())) {
				makeMyWayBack();
			}
			enterModule(Module.Cave);

			caveMapStrategy();
			break;

		case Obstacle:

			setPath(obstacleEntrace);
			if (goTo(getPath())) {
				makeMyWayBack();
			}
			enterModule(Module.Obstacle);
			Button.DOWN.waitForPressAndRelease();

			obstacleMapStrategy();
			break;

		default:
			break;
		}

	}

	/**
	 * 
	 * @return O melhor lugar para se fazer aquela busca marota
	 */
	private static Module bestPlaceToSearch() {
		int centralAmount = 0;
		int caveAmount = 0;
		int obstacleAmount = -4;

		for (int i = 0; i < COL_AMT; i++) {
			for (int j = 0; j < LIN_AMT; j++) {

				if (CENTRAL_MAP[i][j].getStatus() == Status.unchecked && Plano_A.bonecoNoCentro) {

					centralAmount++;

				}

				if (CAVE_MAP[i][j].getStatus() == Status.unchecked) {

					caveAmount++;

				}

				if (OBSTACLE_MAP[i][j].getStatus() == Status.unchecked) {

					obstacleAmount++;

				}

			}
		}

		if (centralAmount >= obstacleAmount || centralAmount >= caveAmount) {

			return Module.Central;

		} else if (caveAmount >= obstacleAmount) {

			return Module.Cave;

		} else {
			return Module.Obstacle;
		}

	}

	/**
	 * Estrat�gia de varredura do mudulo central
	 * 
	 * @throws Exception
	 */
	private static void centralMapStrategy() throws Exception {
		//
		// setPath(point1CentralMap);
		// if (goTo(getPath())) {
		// makeMyWayBack();
		// }
		//
		// setPath(point2CentralMap);
		// if (goTo(getPath())) {
		// makeMyWayBack();
		// }
		//
		// setPath(point3CentralMap);
		// if (goTo(getPath())) {
		// makeMyWayBack();
		// }
		//
		// setPath(point4CentralMap);
		// if (goTo(getPath())) {
		// makeMyWayBack();
		// }

		setPath(new Posicao(1, 1));
		if (goTo(getPath())) {
			makeMyWayBack();
		}

//		setPath(new Posicao(2, 1));
//		if (goTo(getPath())) {
//			makeMyWayBack();
//		}
		setPath(new Posicao(2, 8));
		if (goTo(getPath())) {
			makeMyWayBack();
		}
//		setPath(new Posicao(3, 8));
//		if (goTo(getPath())) {
//			makeMyWayBack();
//		}
		setPath(new Posicao(3, 1));
		if (goTo(getPath())) {
			makeMyWayBack();
		}
//		setPath(new Posicao(4, 1));
//		if (goTo(getPath())) {
//			makeMyWayBack();
//		}
		setPath(new Posicao(4, 8));
		if (goTo(getPath())) {
			makeMyWayBack();
		}
//		setPath(new Posicao(5, 8));
//		if (goTo(getPath())) {
//			makeMyWayBack();
//		}
		setPath(new Posicao(5, 1));
		if (goTo(getPath())) {
			makeMyWayBack();
		}
//		setPath(new Posicao(6, 1));
//		if (goTo(getPath())) {
//			makeMyWayBack();
//		}
		setPath(new Posicao(6, 8));
		if (goTo(getPath())) {
			makeMyWayBack();
		}
//		setPath(new Posicao(7, 8));
//		if (goTo(getPath())) {
//			makeMyWayBack();
//		}
		setPath(new Posicao(7, 1));
		if (goTo(getPath())) {
			makeMyWayBack();
		}
//		setPath(new Posicao(8, 1));
//		if (goTo(getPath())) {
//			makeMyWayBack();
//		}
		setPath(new Posicao(8, 8));
		if (goTo(getPath())) {
			makeMyWayBack();
		}
	}

	/**
	 * Estrat�gia de varredura do modulo da caverna
	 * 
	 * @throws Exception
	 */
	private static void caveMapStrategy() throws Exception {

		setPath(point1Cavemap);
		if (goTo(getPath())) {
			makeMyWayBack();
		}

		setPath(point2Cavemap);
		if (goTo(getPath())) {
			makeMyWayBack();
		}

		setPath(point3Cavemap);
		if (goTo(getPath())) {
			makeMyWayBack();
		}

	}

	private static void checkWall(boolean diagonal) {

	}

	/**
	 * Estrat�gia de varredura do modulo do obstaculo
	 * 
	 * @throws Exception
	 */
	private static void obstacleMapStrategy() throws Exception {
		int contador = 0;

		setPath(point1ObstacleMapS1);
		if (goTo(getPath())) {
			makeMyWayBack();
		}

		if (Sensors.checkIfCellOcuppied()) {
			contador++;
		}
		Navigation.turn(45);

	}

	/**
	 * Reproduz o alegre som de sambar na cara das inimigas
	 */
	private static void victorySong() {
		Sound.setVolume(50);
		Sound.playTone(3000, 100);
		Sound.playTone(4000, 100);
		Sound.playTone(4500, 100);
		Sound.playTone(5000, 100);
		Delay.msDelay(80);
		Sound.playTone(3000, 200);
		Sound.playTone(5000, 500);
	}

	// ======================Logica de captura, mapeamento e retorno============

	private static boolean allowedReading() {

		if (Navigation.robotPosition.x == (LIN_AMT - 1) && (Navigation.orientation == Navigation.FRONT
				|| Navigation.orientation == Navigation.LEFT || Navigation.orientation == Navigation.RIGTH)) {

			return false;

		} else if (Navigation.robotPosition.x == 0 && (Navigation.orientation == Navigation.BACK
				|| Navigation.orientation == Navigation.LEFT || Navigation.orientation == Navigation.RIGTH)) {

			return false;

		} else if ((Navigation.robotPosition.y == (COL_AMT - 1)) && (Navigation.orientation == Navigation.LEFT
				|| Navigation.orientation == Navigation.BACK || Navigation.orientation == Navigation.FRONT)) {

			return false;

		} else if (Navigation.robotPosition.y == 0 && (Navigation.orientation == Navigation.RIGTH
				|| Navigation.orientation == Navigation.BACK || Navigation.orientation == Navigation.FRONT)) {

			return false;

		} 
		//Daqui pra baixo � teste
		else if (Navigation.robotPosition.y == (COL_AMT - 2) && (Navigation.orientation == Navigation.LEFT)) {
			
			return false;
			
		} else if (Navigation.robotPosition.y == 1 && (Navigation.orientation == Navigation.RIGTH)) {
			
			return false;
			
		} else if (Navigation.robotPosition.x == (LIN_AMT - 2) && (Navigation.orientation == Navigation.FRONT)) {
			
			return false;
			
		} else if (Navigation.robotPosition.x == 1 && (Navigation.orientation == Navigation.BACK)) {
			
			return false;
			
		}
		//Aqui termina o teste
		else {

			return true;

		}
	}

	private static void makeMyWayBack() throws Exception {
		switch (getModule()) {
		case Central:
			path.clear();

			setPath(new Posicao(0, 4));
			reverseGoTo(getPath());
			System.out.println("reverse GoTo");
			enterModule(Module.OutOfModule);
			Navigation.openGarra();
			captured = false;

			break;

		case Cave:

			path.clear();

			setPath(new Posicao(0, 4));
			reverseGoTo(getPath());
			enterModule(Module.Central);

			setPath(new Posicao(0, 4));
			reverseGoTo(getPath());
			enterModule(Module.OutOfModule);
			Navigation.openGarra();
			captured = false;

			break;

		case Obstacle:

			path.clear();

			setPath(new Posicao(0, 4));
			reverseGoTo(getPath());
			enterModule(Module.Central);

			setPath(new Posicao(0, 4));
			reverseGoTo(getPath());
			enterModule(Module.OutOfModule);
			Navigation.openGarra();
			captured = false;

			break;

		default:

			break;

		}
	}

	/**
	 * M�todo de captura do alien.<br>
	 * Leitoado, selem as caudas!
	 * 
	 * @return Boolean se de fato houve a captura ou n�o
	 */
	private static boolean captureDoll() {
		Navigation.stop();
		Navigation.setTachometer(false);
		Navigation.globalTacho = Navigation.getTacho("B");

		Navigation.andar(0.08f);

		if (Sensors.verificaObstaculo()) {
			Navigation.andar(0.10f);
			Navigation.closeGarra();
			/*
			 * c�digo de checagem do rog�rio
			 */
			Navigation.andar(-0.18f);
			Navigation.resetTacho();
			Navigation.setTachometer(true);

			return true;
		} else {
			Navigation.andar(-0.05f);
		}

		Navigation.turn(45);

		if (Sensors.verificaObstaculo()) {
			Navigation.andar(0.1f);
			Navigation.closeGarra();
			/*
			 * c�digo de checagem do rog�rio
			 */
			Navigation.andar(-0.10f);
			Navigation.turn(-45);
			Navigation.andar(-0.08f);
			Navigation.resetTacho();
			Navigation.setTachometer(true);

			return true;
		}

		Navigation.turn(-90);

		if (Sensors.verificaObstaculo()) {
			Navigation.andar(0.1f);
			Navigation.closeGarra();
			/*
			 * c�digo de checagem do rog�rio
			 */
			Navigation.andar(-0.10f);
			Navigation.turn(45);
			Navigation.andar(-0.08f);
			Navigation.resetTacho();
			Navigation.setTachometer(true);

			return true;
		} else {
			Navigation.turn(45);
		}

		Navigation.andar(-0.08f);
		Navigation.resetTacho();
		Navigation.setTachometer(true);

		return false;

	}

	/**
	 * Faz a checagem da celula que est� em frente ao robo, usando o sensor de
	 * presen�a e distancia<br>
	 * Caso tenha algo a captura � realizada, caso contr�rio, a c�lula � marcada
	 * como vazia<br>
	 * S� deve ser acionado depois que a leitura for permitida (allowedReading),
	 * para evitar inconsistencias nas leituras
	 * 
	 * @throws Exception
	 */
	private static void checkFrontRobotCell(Celula[][] mapaAtual) throws Exception {

		if (Navigation.orientation == Navigation.FRONT) {
			if (Sensors.checkIfCellOcuppied()) {
				if (captureDoll()) {

					mapaAtual[Navigation.robotPosition.x + 1][Navigation.robotPosition.y].setStatus(Status.empty);
					/*
					 * System.out .println(mapaAtual[Navigation.robotPosition.x
					 * + 1][Navigation.robotPosition.y].getStatus());
					 */

					captured = true;

				} else {

					mapaAtual[Navigation.robotPosition.x + 1][Navigation.robotPosition.y].setStatus(Status.occupied);

				}

			} else {

				mapaAtual[Navigation.robotPosition.x + 1][Navigation.robotPosition.y].setStatus(Status.empty);
				/*
				 * System.out.println(mapaAtual[Navigation.robotPosition.x +
				 * 1][Navigation.robotPosition.y].getStatus());
				 */
			}

		}

		else if (Navigation.orientation == Navigation.BACK) {

			if (Sensors.checkIfCellOcuppied()) {
				if (captureDoll()) {

					mapaAtual[Navigation.robotPosition.x - 1][Navigation.robotPosition.y].setStatus(Status.empty);

					/*
					 * System.out .println(mapaAtual[Navigation.robotPosition.x
					 * - 1][Navigation.robotPosition.y].getStatus());
					 */

					captured = true;

				} else {

					mapaAtual[Navigation.robotPosition.x - 1][Navigation.robotPosition.y].setStatus(Status.occupied);

				}
			} else {

				mapaAtual[Navigation.robotPosition.x - 1][Navigation.robotPosition.y].setStatus(Status.empty);
				/*
				 * System.out.println(mapaAtual[Navigation.robotPosition.x -
				 * 1][Navigation.robotPosition.y].getStatus());
				 */

			}

		}

		else if (Navigation.orientation == Navigation.LEFT) {
			if (Sensors.checkIfCellOcuppied()) {
				if (captureDoll()) {

					mapaAtual[Navigation.robotPosition.x][Navigation.robotPosition.y + 1].setStatus(Status.empty);
					/*
					 * System.out
					 * .println(mapaAtual[Navigation.robotPosition.x][Navigation
					 * .robotPosition.y + 1].getStatus());
					 */

					captured = true;

				} else {

					mapaAtual[Navigation.robotPosition.x][Navigation.robotPosition.y + 1].setStatus(Status.occupied);

				}

			} else {

				mapaAtual[Navigation.robotPosition.x][Navigation.robotPosition.y + 1].setStatus(Status.empty);
				/*
				 * System.out.println(mapaAtual[Navigation.robotPosition.x][
				 * Navigation.robotPosition.y + 1].getStatus());
				 */

			}

		}

		else if (Navigation.orientation == Navigation.RIGTH) {
			if (Sensors.checkIfCellOcuppied()) {
				if (captureDoll()) {

					mapaAtual[Navigation.robotPosition.x][Navigation.robotPosition.y - 1].setStatus(Status.empty);
					/*
					 * System.out
					 * .println(mapaAtual[Navigation.robotPosition.x][Navigation
					 * .robotPosition.y - 1].getStatus());
					 */

					captured = true;

				} else {

					mapaAtual[Navigation.robotPosition.x][Navigation.robotPosition.y - 1].setStatus(Status.occupied);

				}
			} else {
				mapaAtual[Navigation.robotPosition.x][Navigation.robotPosition.y - 1].setStatus(Status.empty);
				/*
				 * System.out.println(mapaAtual[Navigation.robotPosition.x][
				 * Navigation.robotPosition.y - 1].getStatus());
				 */

			}

		}
	}

	public static void iniciaPerifericoFrenteDireita(int cave) {
		/*
		 * Chama o launcher dos mapas
		 */
		mapLauncher();

		switch (cave) {

		case Plano_A.CAV_DIR:

			Posicao temp1 = new Posicao(4, 0);
			caveEntrance = temp1;
			Posicao temp2 = new Posicao(8, 4);
			obstacleEntrace = temp2;

			caveExit = new Posicao(0, 4);
			obstacleExit = new Posicao(0, 4);
			break;

		case Plano_A.CAV_CIMA:
			Posicao temp3 = new Posicao(8, 4);
			caveEntrance = temp3;
			Posicao temp4 = new Posicao(4, 0);
			obstacleEntrace = temp4;

			caveExit = new Posicao(0, 4);
			obstacleExit = new Posicao(0, 4);
			break;
		}

	}

	public static void iniciaPerifericoFrenteEsquerda(int cave) {
		/*
		 * chama o launcher dos mapas
		 */
		mapLauncher();

		switch (cave) {

		case Plano_A.CAV_CIMA:
			Posicao temp1 = new Posicao(8, 4);
			caveEntrance = temp1;
			Posicao temp2 = new Posicao(4, 8);
			obstacleEntrace = temp2;

			caveExit = new Posicao(0, 4);
			obstacleExit = new Posicao(0, 4);

			break;

		case Plano_A.CAV_ESQ:
			Posicao temp3 = new Posicao(4, 8);
			caveEntrance = temp3;
			Posicao temp4 = new Posicao(8, 4);
			obstacleEntrace = temp4;

			caveExit = new Posicao(0, 4);
			obstacleExit = new Posicao(0, 4);

			break;
		}

	}

	public static void iniciaPerifericoLateral(int cave) {
		/*
		 * chama o launcher dos mapas
		 */
		mapLauncher();

		switch (cave) {

		case Plano_A.CAV_ESQ:
			Posicao temp1 = new Posicao(4, 8);
			caveEntrance = temp1;
			Posicao temp2 = new Posicao(4, 0);
			obstacleEntrace = temp2;

			caveExit = new Posicao(0, 4);
			obstacleExit = new Posicao(0, 4);

			break;

		case Plano_A.CAV_DIR:
			Posicao temp3 = new Posicao(4, 0);
			caveEntrance = temp3;
			Posicao temp4 = new Posicao(4, 8);
			obstacleEntrace = temp4;

			caveExit = new Posicao(0, 4);
			obstacleExit = new Posicao(0, 4);

			break;
		}
	}

	private static void mapLauncher() {
		/*
		 * Inicia o modulo central e perifericos com todas as celulas como n�o
		 * checadas com excess�o das celulas centrais do modulo caverna.
		 */
		for (int i = 0; i < LIN_AMT; i++) {
			for (int j = 0; j < COL_AMT; j++) {

				if (i == 0 || i == (LIN_AMT - 1) || j == 0 || j == (COL_AMT - 1)) {
					CENTRAL_MAP[i][j] = new Celula(new Posicao(i, j), Status.unchecked);
					CENTRAL_MAP[i][j].g = 1;

					OBSTACLE_MAP[i][j] = new Celula(new Posicao(i, j), Status.unchecked);
					OBSTACLE_MAP[i][j].g = 1;

					if ((i >= 3 && i <= 5) && (j >= 3 && j <= 5)) {

						CAVE_MAP[i][j] = new Celula(new Posicao(i, j), Status.occupied);
						CAVE_MAP[i][j].g = 1;

					} else {

						CAVE_MAP[i][j] = new Celula(new Posicao(i, j), Status.unchecked);
						CAVE_MAP[i][j].g = 1;

					}
				} else {
					CENTRAL_MAP[i][j] = new Celula(new Posicao(i, j), Status.unchecked);

					OBSTACLE_MAP[i][j] = new Celula(new Posicao(i, j), Status.unchecked);

					if ((i >= 2 && i <= 6) && (j >= 2 && j <= 6)) {

						CAVE_MAP[i][j] = new Celula(new Posicao(i, j), Status.occupied);

					} else {

						CAVE_MAP[i][j] = new Celula(new Posicao(i, j), Status.unchecked);

					}
				}

			}
		}
	}

	/**
	 * Garante que a orienta��o do robo est� correta para mudar de um modulo ao
	 * outro<br>
	 * S� usar dentro do m�todo de troca de modulo <b>enterModule</b>
	 */
	private static void makeSureOrientationSRight() {
		if (Navigation.robotPosition.x == 0) {

			while (Navigation.orientation != Navigation.BACK) {
				if (Navigation.orientation == Navigation.LEFT) {

					Navigation.turn(90);

				} else if (Navigation.orientation == Navigation.RIGTH) {

					Navigation.turn(-90);

				} else {

					Navigation.turn(90);

				}
			}

		} else if (Navigation.robotPosition.x == (COL_AMT - 1)) {

			while (Navigation.orientation != Navigation.FRONT) {

				if (Navigation.orientation == Navigation.LEFT) {

					Navigation.turn(-90);

				} else if (Navigation.orientation == Navigation.RIGTH) {

					Navigation.turn(90);

				} else {

					Navigation.turn(-90);

				}
			}

		} else if (Navigation.robotPosition.y == 0) {

			while (Navigation.orientation != Navigation.RIGTH) {

				if (Navigation.orientation == Navigation.FRONT) {

					Navigation.turn(-90);

				} else if (Navigation.orientation == Navigation.BACK) {

					Navigation.turn(90);

				} else {

					Navigation.turn(90);

				}
			}

		} else if (Navigation.robotPosition.y == (LIN_AMT - 1)) {

			while (Navigation.orientation != Navigation.LEFT) {

				if (Navigation.orientation == Navigation.FRONT) {

					Navigation.turn(90);

				} else if (Navigation.orientation == Navigation.BACK) {

					Navigation.turn(-90);

				} else {

					Navigation.turn(90);
				}

			}

		}
	}

	/**
	 * Realiza a troca de modulo, bem como a atualiza��o de dire��o e
	 * posicionamento nas celulas
	 * 
	 * @param moduloAlvo
	 *            Modulo ao qual se deseja alcan�ar
	 */
	private static void enterModule(Module moduloAlvo) {
		/*
		 * Quando se deseja entrar no m�dulo central, vindo da area de resgate
		 * chamada OutOfModule
		 */
		if (modulo == Module.OutOfModule && moduloAlvo == Module.Central) {

			Navigation.andar(DIST_ENTRADA);

			setModule(moduloAlvo);

			Navigation.resetTacho();

			Navigation.orientation = Navigation.FRONT;

			Navigation.robotPosition = new Posicao(0, 4);

			Navigation.setTachometer(true);

		}
		/*
		 * Quando se quer entrar no modulo da caverna
		 */
		else if (modulo == Module.Central && moduloAlvo == Module.Cave) {

			if (Navigation.orientation == Navigation.FRONT) {

				orientacaoArmazenada = Navigation.BACK;

			} else if (Navigation.orientation == Navigation.BACK) {

				orientacaoArmazenada = Navigation.FRONT;

			} else if (Navigation.orientation == Navigation.LEFT) {

				orientacaoArmazenada = Navigation.RIGTH;

			} else if (Navigation.orientation == Navigation.RIGTH) {

				orientacaoArmazenada = Navigation.LEFT;

			}

			Navigation.setTachometer(false);

			makeSureOrientationSRight();

			Navigation.andar(DIST_TRAVESSIA);

			setModule(moduloAlvo);

			Navigation.resetTacho();

			Navigation.orientation = Navigation.FRONT;

			Navigation.robotPosition = new Posicao(0, 4);
			;

			Navigation.setTachometer(true);

		}
		/*
		 * Quando se quer entrar no modulo do obst�culo
		 */
		else if (modulo == Module.Central && moduloAlvo == Module.Obstacle) {
			if (Navigation.orientation == Navigation.FRONT) {

				orientacaoArmazenada = Navigation.BACK;

			} else if (Navigation.orientation == Navigation.BACK) {

				orientacaoArmazenada = Navigation.FRONT;

			} else if (Navigation.orientation == Navigation.LEFT) {

				orientacaoArmazenada = Navigation.RIGTH;

			} else if (Navigation.orientation == Navigation.RIGTH) {

				orientacaoArmazenada = Navigation.LEFT;

			}

			Navigation.setTachometer(false);

			makeSureOrientationSRight();

			Navigation.andar(DIST_TRAVESSIA);

			setModule(moduloAlvo);

			Navigation.resetTacho();

			Navigation.orientation = Navigation.FRONT;

			Navigation.robotPosition = new Posicao(0, 4);
			;

			Navigation.setTachometer(true);

		}
		/*
		 * Quando se quer entrar no modulo central vindo do modulo cave
		 */
		else if (modulo == Module.Cave && moduloAlvo == Module.Central) {

			Navigation.setTachometer(false);

			makeSureOrientationSRight();

			Navigation.andar(DIST_TRAVESSIA);

			setModule(moduloAlvo);

			Navigation.resetTacho();

			Navigation.orientation = orientacaoArmazenada;

			Navigation.robotPosition = caveEntrance;

			Navigation.setTachometer(true);

		}
		/*
		 * Quando se quer entrar no mudulo central vindo do modulo Obstacle
		 */
		else if (modulo == Module.Obstacle && moduloAlvo == Module.Central) {

			Navigation.setTachometer(false);

			makeSureOrientationSRight();

			Navigation.andar(DIST_TRAVESSIA);

			setModule(moduloAlvo);

			Navigation.resetTacho();

			Navigation.orientation = orientacaoArmazenada;

			Navigation.robotPosition = obstacleEntrace;

			Navigation.setTachometer(true);

		}
		/*
		 * Quando se deseja entrar na area de resgate chamado OutOfModule
		 */
		else if (modulo == Module.Central && moduloAlvo == Module.OutOfModule) {

			Navigation.setTachometer(false);

			makeSureOrientationSRight();

			Navigation.andar(DIST_SAIDA);

			setModule(moduloAlvo);

			Navigation.resetTacho();

		}
	}

	/**
	 * Retorna o modulo atual em que o robo est�
	 * 
	 * @return Modulo, do tipo <b>Module</b>
	 */
	public static Module getModule() {

		return modulo;

	}

	/**
	 * Permite que o modulo em que o robo est� seja alterado
	 * 
	 * @param moduloAlvo
	 *            Do tipo <b>Module</b>
	 */
	public static void setModule(Module moduloAlvo) {

		modulo = moduloAlvo;

	}

	private static Celula[][] currentMap() {
		switch (getModule()) {

		case Central:

			return CENTRAL_MAP;

		case Cave:

			return CAVE_MAP;

		case Obstacle:

			return OBSTACLE_MAP;

		default:
			break;
		}
		return null;
	}

	/**
	 * Literalmente encontra o melhor caminho para passar e seta esse caminho em
	 * <b>path</b>
	 * 
	 * @param posicaoAlvo
	 *            posi��o ao qual se deseja chegar
	 * @throws Exception
	 *             Exce��o gerada pelo uso do A*
	 */
	private static void setPath(Posicao posicaoAlvo) throws Exception {
		aStar = new Astar(currentMap());
		AlienRescue.path = aStar.search(Navigation.robotPosition, posicaoAlvo);
		System.out.println(path.isEmpty());

	}

	private static void setReversePath(Posicao posicaoAlvo) throws Exception {
		aStar = new Astar(currentMap());
		AlienRescue.path = aStar.searchReversePath(Navigation.robotPosition, posicaoAlvo);
	}

	/**
	 * Retorna a lista de celulas que formam o melhor caminho para uma posi��o
	 * alvo, que foi definidas dentro de path
	 * 
	 * @return O caminho que est� contido dentro do <b>path</b>
	 */
	private static List<Celula> getPath() {

		return AlienRescue.path;

	}

	/**
	 * Esse m�todo implementa o retornar do rob� de sua posi��o para uma posi��o
	 * alvo
	 * 
	 * @param caminho
	 *            � uma lista de celulas de por onde o robo deve passar para
	 *            chegar na posi��o alvo
	 * @throws Exception
	 */
	private static void reverseGoTo(List<Celula> caminho) throws Exception {
		/*
		 * if (caminho.isEmpty()) { System.out.println("Caminho Vazio");
		 * Sound.buzz(); } else {
		 */

		for (int i = 0; i < caminho.size(); i++) {
			/*
			 * A c�lula est� a esquerda da posi��o do robounico modo de chegar
			 * at� ela � s� quando a orienta��o for LEFT
			 */
			if (caminho.get(i).getPosicao().x == Navigation.robotPosition.x
					&& caminho.get(i).getPosicao().y > Navigation.robotPosition.y) {

				while (Navigation.orientation != Navigation.LEFT) {

					if (Navigation.orientation == Navigation.FRONT) {

						Navigation.stop();
						Navigation.turn(90);

					} else if (Navigation.orientation == Navigation.BACK) {

						Navigation.stop();
						Navigation.turn(-90);

					} else {

						Navigation.stop();
						Navigation.turn(90);
					}

				}

				Navigation.forward();

				while (true) {

					if (cellExchanged == false) {

					} else {

						if ((i + 1) < caminho.size()) {
							Sound.beep();

							cellExchanged = false;
							break;

						} else {

							Navigation.stop();

							cellExchanged = false;
							break;

						}
					}
				}

			}
			/*
			 * A c�lula est� a direita da posi��o do robo, unico modo de chegar
			 * at� ela � s� quando a orienta��o for RIGHT
			 */
			else if (caminho.get(i).getPosicao().x == Navigation.robotPosition.x
					&& caminho.get(i).getPosicao().y < Navigation.robotPosition.y) {

				while (Navigation.orientation != Navigation.RIGTH) {

					if (Navigation.orientation == Navigation.FRONT) {

						Navigation.stop();
						Navigation.turn(-90);

					} else if (Navigation.orientation == Navigation.BACK) {

						Navigation.stop();
						Navigation.turn(90);

					} else {

						Navigation.stop();
						Navigation.turn(90);

					}

				}

				Navigation.forward();

				while (true) {

					if (cellExchanged == false) {

					} else {

						if ((i + 1) < caminho.size()) {
							Sound.beep();

							cellExchanged = false;
							break;

						} else {
							Navigation.stop();

							cellExchanged = false;
							break;

						}
					}
				}

			}
			/*
			 * A c�lula est� a frente da posi��o do robo, unico modo de chegar
			 * at� ela � s� quando a orienta��o for FRONT
			 */
			else if (caminho.get(i).getPosicao().x > Navigation.robotPosition.x
					&& caminho.get(i).getPosicao().y == Navigation.robotPosition.y) {

				while (Navigation.orientation != Navigation.FRONT) {

					if (Navigation.orientation == Navigation.LEFT) {

						Navigation.stop();
						Navigation.turn(-90);

					} else if (Navigation.orientation == Navigation.RIGTH) {

						Navigation.stop();
						Navigation.turn(90);

					} else {

						Navigation.stop();
						Navigation.turn(-90);

					}

				}

				Navigation.forward();

				while (true) {

					if (cellExchanged == false) {

					} else {

						if ((i + 1) < caminho.size()) {
							Sound.beep();

							cellExchanged = false;
							break;

						} else {
							Navigation.stop();

							cellExchanged = false;
							break;

						}
					}
				}

			}
			/*
			 * A c�lula est� atr�s da posi��o do robo, unico modo de chegar at�
			 * ela � s� quando a orienta��o for BACK
			 */
			else if (caminho.get(i).getPosicao().x < Navigation.robotPosition.x
					&& caminho.get(i).getPosicao().y == Navigation.robotPosition.y) {

				while (Navigation.orientation != Navigation.BACK) {

					if (Navigation.orientation == Navigation.LEFT) {

						Navigation.stop();
						Navigation.turn(90);

					} else if (Navigation.orientation == Navigation.RIGTH) {

						Navigation.stop();
						Navigation.turn(-90);

					} else {

						Navigation.stop();
						Navigation.turn(90);

					}

				}

				Navigation.forward();

				while (true) {

					if (cellExchanged == false) {

					} else {

						if ((i + 1) < caminho.size()) {
							Sound.beep();
							cellExchanged = false;
							break;

						} else {

							Navigation.stop();

							cellExchanged = false;
							break;

						}
					}
				}

			}
		}
	}
	/* } */

	/**
	 * Esse m�todo implementa o ir do rob� de sua posi��o para uma posi��o alvo
	 * 
	 * @param caminho
	 *            � uma lista de celulas de por onde o robo deve passar para
	 *            chegar na posi��o alvo
	 * @throws Exception
	 */
	private static boolean goTo(List<Celula> caminho) throws Exception {

		boolean captureReturn = false;
		if (caminho.isEmpty()) {
			System.out.println("Caminho Vazio");
			Sound.buzz();
		} else {

			for (int i = 0; i < caminho.size(); i++) {
				/*
				 * A c�lula est� a esquerda da posi��o do robounico modo de
				 * chegar at� ela � s� quando a orienta��o for LEFT
				 */
				if (caminho.get(i).getPosicao().x == Navigation.robotPosition.x
						&& caminho.get(i).getPosicao().y > Navigation.robotPosition.y) {

					while (Navigation.orientation != Navigation.LEFT) {

						if (Navigation.orientation == Navigation.FRONT) {

							Navigation.stop();
							Navigation.turn(90);

						} else if (Navigation.orientation == Navigation.BACK) {

							Navigation.stop();
							Navigation.turn(-90);

						} else {

							Navigation.stop();
							Navigation.turn(90);
						}

					}

					Navigation.forward();

					while (true) {

						if (cellExchanged == false) {
							if (!captured && allowedReading()/* && !cellAlreadyRead */) {

								checkFrontRobotCell(currentMap());
								cellAlreadyRead = true;

							}
						} else {

							if ((i + 1) < caminho.size()) {
								Sound.beep();

								cellExchanged = false;
								break;

							} else {

								Navigation.stop();

								cellExchanged = false;
								break;

							}
						}
					}

					if (captured == true) {
						Navigation.stop();
						captureReturn = true;
						break;
					} else {
						continue;
					}

				}
				/*
				 * A c�lula est� a direita da posi��o do robo, unico modo de
				 * chegar at� ela � s� quando a orienta��o for RIGHT
				 */
				else if (caminho.get(i).getPosicao().x == Navigation.robotPosition.x
						&& caminho.get(i).getPosicao().y < Navigation.robotPosition.y) {

					while (Navigation.orientation != Navigation.RIGTH) {

						if (Navigation.orientation == Navigation.FRONT) {

							Navigation.stop();
							Navigation.turn(-90);

						} else if (Navigation.orientation == Navigation.BACK) {

							Navigation.stop();
							Navigation.turn(90);

						} else {

							Navigation.stop();
							Navigation.turn(90);

						}

					}

					Navigation.forward();

					while (true) {

						if (cellExchanged == false) {
							if (!captured
									&& allowedReading() /*
														 * && !cellAlreadyRead
														 */) {

								checkFrontRobotCell(currentMap());
								cellAlreadyRead = true;

							}
						} else {

							if ((i + 1) < caminho.size()) {
								Sound.beep();

								cellExchanged = false;
								break;

							} else {
								Navigation.stop();

								cellExchanged = false;
								break;

							}
						}
					}

					if (captured == true) {
						Navigation.stop();
						captureReturn = true;
						break;
					} else {
						continue;
					}

				}
				/*
				 * A c�lula est� a frente da posi��o do robo, unico modo de
				 * chegar at� ela � s� quando a orienta��o for FRONT
				 */
				else if (caminho.get(i).getPosicao().x > Navigation.robotPosition.x
						&& caminho.get(i).getPosicao().y == Navigation.robotPosition.y) {

					while (Navigation.orientation != Navigation.FRONT) {

						if (Navigation.orientation == Navigation.LEFT) {

							Navigation.stop();
							Navigation.turn(-90);

						} else if (Navigation.orientation == Navigation.RIGTH) {

							Navigation.stop();
							Navigation.turn(90);

						} else {

							Navigation.stop();
							Navigation.turn(-90);

						}

					}

					Navigation.forward();

					while (true) {

						if (cellExchanged == false) {
							if (!captured
									&& allowedReading() /*
														 * && !cellAlreadyRead
														 */) {

								checkFrontRobotCell(currentMap());
								cellAlreadyRead = true;

							}
						} else {

							if ((i + 1) < caminho.size()) {
								Sound.beep();

								cellExchanged = false;
								break;

							} else {
								Navigation.stop();

								cellExchanged = false;
								break;

							}
						}
					}
					if (captured == true) {
						Navigation.stop();
						captureReturn = true;
						break;
					} else {
						continue;
					}

				}
				/*
				 * A c�lula est� atr�s da posi��o do robo, unico modo de chegar
				 * at� ela � s� quando a orienta��o for BACK
				 */
				else if (caminho.get(i).getPosicao().x < Navigation.robotPosition.x
						&& caminho.get(i).getPosicao().y == Navigation.robotPosition.y) {

					while (Navigation.orientation != Navigation.BACK) {

						if (Navigation.orientation == Navigation.LEFT) {

							Navigation.stop();
							Navigation.turn(90);

						} else if (Navigation.orientation == Navigation.RIGTH) {

							Navigation.stop();
							Navigation.turn(-90);

						} else {

							Navigation.stop();
							Navigation.turn(90);

						}

					}

					Navigation.forward();

					while (true) {

						if (cellExchanged == false) {
							if (!captured
									&& allowedReading() /*
														 * && !cellAlreadyRead
														 */) {

								checkFrontRobotCell(currentMap());
								cellAlreadyRead = true;

							}
						} else {

							if ((i + 1) < caminho.size()) {
								Sound.beep();
								cellExchanged = false;
								break;

							} else {

								Navigation.stop();

								cellExchanged = false;
								break;

							}
						}
					}
					if (captured == true) {
						Navigation.stop();
						captureReturn = true;
						break;
					} else {
						continue;
					}
				}
			}
		}

		return captureReturn;
	}

}