package cut.link.service

import java.util.Base64

import cut.link.flow.LinkMessageFlow
import cut.link.model.Link

case class LinkService(linkMessageFlow: LinkMessageFlow) {
  private val base64Encoder = Base64.getUrlEncoder

  def cutLink(uri: String): Link = {
    val id   = createLinkId(uri)
    val link = Link(id, uri)
    scribe.info(s"Created a link: $link")
    linkMessageFlow.sendLinkMessage(link)
    link
  }

  private def createLinkId(uri: String) = {
    val hashcode      = uri.hashCode
    val hashcodeBytes = BigInt(hashcode).toByteArray
    // Drops '=' chars which are the result of base64 padding
    base64Encoder.encodeToString(hashcodeBytes).reverse.dropWhile(_ == '=')
  }
}
