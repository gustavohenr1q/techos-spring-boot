
package br.com.bugnacio.techos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class FotoStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 12_000_000L;

    private final Path diretorio;

    public record FotoSalva(
            String nomeArquivo,
            String contentType
    ) {}

    public FotoStorageService(
            @Value("${techos.fotos.diretorio:${user.home}/techos-fotos}")
            String caminho) {

        this.diretorio = Path.of(caminho)
                .toAbsolutePath()
                .normalize();
    }

    public FotoSalva salvar(MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selecione uma foto."
            );
        }

        if (arquivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "A foto deve ter no máximo 5 MB."
            );
        }

        try (
                InputStream entrada = arquivo.getInputStream();
                ImageInputStream imagemEntrada =
                        ImageIO.createImageInputStream(entrada)
        ) {

            if (imagemEntrada == null) {
                throw new IllegalArgumentException(
                        "Não foi possível ler a imagem."
                );
            }

            Iterator<ImageReader> leitores =
                    ImageIO.getImageReaders(imagemEntrada);

            if (!leitores.hasNext()) {
                throw new IllegalArgumentException(
                        "Arquivo inválido. Envie JPEG ou PNG."
                );
            }

            ImageReader leitor = leitores.next();

            BufferedImage imagem;
            String formato;

            try {
                leitor.setInput(imagemEntrada);

                formato = leitor.getFormatName()
                        .toLowerCase(Locale.ROOT);

                if (!formato.equals("jpeg")
                        && !formato.equals("jpg")
                        && !formato.equals("png")) {

                    throw new IllegalArgumentException(
                            "Formato não permitido. Envie JPEG ou PNG."
                    );
                }

                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);

                if (largura <= 0 || altura <= 0
                        || (long) largura * altura > MAX_PIXELS) {

                    throw new IllegalArgumentException(
                            "A imagem possui dimensões inválidas."
                    );
                }

                imagem = leitor.read(0);

            } finally {
                leitor.dispose();
            }

            boolean jpeg = formato.equals("jpeg")
                    || formato.equals("jpg");

            String extensao = jpeg ? ".jpg" : ".png";
            String formatoSaida = jpeg ? "jpg" : "png";
            String contentType = jpeg
                    ? "image/jpeg"
                    : "image/png";

            // Recria a imagem para não armazenar
            // diretamente o arquivo recebido.
            BufferedImage limpa = new BufferedImage(
                    imagem.getWidth(),
                    imagem.getHeight(),
                    jpeg
                            ? BufferedImage.TYPE_INT_RGB
                            : BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = limpa.createGraphics();

            try {
                if (jpeg) {
                    g.setColor(Color.WHITE);
                    g.fillRect(
                            0, 0,
                            limpa.getWidth(),
                            limpa.getHeight()
                    );
                }

                g.drawImage(imagem, 0, 0, null);

            } finally {
                g.dispose();
            }

            Files.createDirectories(diretorio);

            String nome = UUID.randomUUID() + extensao;
            Path destino = diretorio.resolve(nome);

            try (OutputStream saida = Files.newOutputStream(
                    destino,
                    StandardOpenOption.CREATE_NEW)) {

                if (!ImageIO.write(
                        limpa,
                        formatoSaida,
                        saida)) {

                    throw new IOException(
                            "Não foi possível gravar a imagem."
                    );
                }

            } catch (IOException ex) {
                Files.deleteIfExists(destino);
                throw ex;
            }

            return new FotoSalva(nome, contentType);

        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Falha ao armazenar a foto.",
                    ex
            );
        }
    }

    public byte[] ler(String nomeArquivo) {

        Path arquivo = resolver(nomeArquivo);

        try {
            return Files.readAllBytes(arquivo);

        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Não foi possível recuperar a foto.",
                    ex
            );
        }
    }

    public void remover(String nomeArquivo) {

        Path arquivo = resolver(nomeArquivo);

        try {
            Files.deleteIfExists(arquivo);

        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Não foi possível remover a foto.",
                    ex
            );
        }
    }

    private Path resolver(String nomeArquivo) {

        if (nomeArquivo == null
                || !nomeArquivo.matches(
                        "[0-9a-f-]{36}\\.(jpg|png)")) {

            throw new IllegalArgumentException(
                    "Nome de arquivo inválido."
            );
        }

        Path arquivo = diretorio.resolve(
                nomeArquivo
        ).normalize();

        if (!arquivo.startsWith(diretorio)) {
            throw new IllegalArgumentException(
                    "Caminho de arquivo inválido."
            );
        }

        return arquivo;
    }
}
